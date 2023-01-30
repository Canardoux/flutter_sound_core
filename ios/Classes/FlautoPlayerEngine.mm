/*
 * Copyright 2018, 2019, 2020, 2021 Dooboolab.
 *
 * This file is part of Flutter-Sound.
 *
 * Flutter-Sound is free software: you can redistribute it and/or modify
 * it under the terms of the Mozilla Public License version 2 (MPL2.0),
 * as published by the Mozilla organization.
 *
 * Flutter-Sound is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * MPL General Public License for more details.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */


//
//  PlayerEngine.h
//  Pods
//
//  Created by larpoux on 03/09/2020.
//
#import "Flauto.h"
#import "FlautoPlayerEngine.h"
#import "FlautoPlayer.h"
#import <Flutter/Flutter.h>

@implementation AudioPlayerFlauto
{
        FlautoPlayer* flautoPlayer; // Owner
        AVAudioPlayer* player;
}

       - (AVAudioPlayer*) getAudioPlayer
       {
                return player;
       }

        - (void) setAudioPlayer: (AVAudioPlayer*)thePlayer
        {
                player = thePlayer;
        }



       - (AudioPlayerFlauto*)init: (FlautoPlayer*)owner
       {
                flautoPlayer = owner;
                return [super init];
       }

       -(void) startPlayerFromBuffer: (NSData*) dataBuffer
       {
                NSError* error = [[NSError alloc] init];
                [self setAudioPlayer:  [[AVAudioPlayer alloc] initWithData: dataBuffer error: &error]];
                [self getAudioPlayer].delegate = flautoPlayer;
       }

       -(void)  startPlayerFromURL: (NSURL*) url codec: (t_CODEC)codec channels: (int)numChannels sampleRate: (long)sampleRate

       {
                [self setAudioPlayer: [[AVAudioPlayer alloc] initWithContentsOfURL: url error: nil] ];
                [self getAudioPlayer].delegate = flautoPlayer;
        }


       -(long)  getDuration
       {
                double duration =  [self getAudioPlayer].duration;
                return (long)(duration * 1000.0);
       }

       -(long)  getPosition
       {
                double position = [self getAudioPlayer].currentTime ;
                return (long)( position * 1000.0);
       }

       -(void)  stop
       {
                [ [self getAudioPlayer] stop];
                [self setAudioPlayer: nil];
       }

        -(bool)  play
        {
                bool b = [ [self getAudioPlayer] play];
                return b;
        }


       -(bool)  resume
       {
                bool b = [ [self getAudioPlayer] play];
                return b;
       }

       -(bool)  pause
       {
                [ [self getAudioPlayer] pause];
                return true;
       }


       -(bool)  setVolume: (double) volume fadeDuration:(NSTimeInterval)fadeDuration // volume is between 0.0 and 1.0
       {
               if (fadeDuration == 0)
                     [ [self getAudioPlayer] setVolume: volume ];
               else
                       [ [self getAudioPlayer] setVolume: volume fadeDuration: fadeDuration];
               return true;
       }


        -(bool)  setSpeed: (double) speed // speed is between 0.0 and 1.0 to go slower
        {
                [self getAudioPlayer].enableRate = true ; // Probably not always !!!!
                [self getAudioPlayer].rate = speed ;
                return true;
        }

       -(bool)  seek: (double) pos
       {
                [self getAudioPlayer].currentTime = pos / 1000.0;
                return true;
       }

       -(t_PLAYER_STATE)  getStatus
       {
                if (  [self getAudioPlayer] == nil )
                        return PLAYER_IS_STOPPED;
                if ( [ [self getAudioPlayer] isPlaying])
                        return PLAYER_IS_PLAYING;
                return PLAYER_IS_PAUSED;
       }


        - (int) feed: (NSData*)data
        {
                return -1;
        }

@end


// ---------------------------------------------------------------------------------------------------------------------------------------------------------------


@implementation AudioEngine
{
        FlautoPlayer* flutterSoundPlayer; // Owner
        AVAudioEngine* engine;
        AVAudioUnitEQ* eq;
        AVAudioPlayerNode* playerNode;
        AVAudioFormat* playerFormat;
        AVAudioFormat* outputFormat;
        AVAudioOutputNode* outputNode;
        AVAudioConverter* converter;
        CFTimeInterval mStartPauseTime ; // The time when playback was paused
	CFTimeInterval systemTime ; //The time when  StartPlayer() ;
        double mPauseTime ; // The number of seconds during the total Pause mode
        NSData* waitingBlock;
        long m_sampleRate ;
        int  m_numChannels;
}

       - (AudioEngine*)init: (FlautoPlayer*)owner
       {
                flutterSoundPlayer = owner;
                waitingBlock = nil;
                engine = [[AVAudioEngine alloc] init];
                outputNode = [engine outputNode];
           
                if (@available(iOS 13.0, *)) {
                    if ([flutterSoundPlayer isVoiceProcessingEnabled]) {
                        NSError* err;
                        if (![outputNode setVoiceProcessingEnabled:YES error:&err]) {
                           [flutterSoundPlayer logDebug:[NSString stringWithFormat:@"error enabling voiceProcessing => %@", err]];
                        } else {
                            [flutterSoundPlayer logDebug: @"VoiceProcessing enabled"];
                        }
                    }
                } else {
                   [flutterSoundPlayer logDebug: @"WARNING! VoiceProcessing is only available on iOS13+"];
                }
               
                outputFormat = [outputNode inputFormatForBus: 0];
                playerNode = [[AVAudioPlayerNode alloc] init];

                [engine attachNode: playerNode];

                [engine connect: playerNode to: outputNode format: outputFormat];
                bool b = [engine startAndReturnError: nil];
                if (!b)
                {
                        [flutterSoundPlayer logDebug: @"Cannot start the audio engine"];
                }

                mPauseTime = 0.0; // Total number of seconds in pause mode
		mStartPauseTime = -1; // Not in paused mode
		systemTime = CACurrentMediaTime(); // The time when started
                return [super init];
       }

       - (void) attachEqualizer: (AVAudioUnitEQ*) eq
       {
                [engine attachNode: eq];
                [engine connect: playerNode to: eq format: playerFormat];
                [engine connect: eq to: outputNode format: outputFormat];
       }

       - (void) detachEqualizer: (AVAudioUnitEQ*) eq
       {
                [engine disconnectNodeInput: eq];
                [engine disconnectNodeInput: playerNode];
                [engine disconnectNodeInput: outputNode];
                [engine connect: playerNode to: outputNode format: outputFormat];
                [engine detachNode: eq];
       }

       - (void) enableEffect: (String) type, bool enabled
       {
                switch (type)
                {
                        case "DarwinEqualizer":
                                
                                if (eq == nil)
                                {
                                        [flutterSoundPlayer logDebug: @"Cannot enable the equalizer because it is not initialized"];
                                        return;
                                }
                                eq.bypass = !enabled;
                                
                                break;
                        default:

                                break;
                }
       }

       - (void) setEqiualizerBandGain: (int) bandIndex gain: (double) gain
       {
                if (eq == nil)
                {
                        [flutterSoundPlayer logDebug: @"Cannot set the equalizer band gain because the equalizer is not initialized"];
                        return;
                }
                if (bandIndex < 0 || bandIndex >= eq.bands.count)
                {
                        [flutterSoundPlayer logDebug: @"Cannot set the equalizer band gain because the band index is out of range"];
                        return;
                }
                AVAudioUnitEQFilterParameters* band = eq.bands[bandIndex];
                band.gain = gain;
       }

       - (void)initEqualizer:(FlutterMethodCall *)call result:(FlutterResult)result {
                [flutterSoundPlayer logDebug:@"Initialize darwin EQ!"];
                NSDictionary *arguments = call.arguments;
                if (![arguments isKindOfClass:[NSDictionary class]]) {
                        result([FlutterError errorWithCode:@"ABNORMAL_PARAMETER" message:@"no parameters" details:nil]);
                        return;
                }
                
                [flutterSoundPlayer logDebug:@"%@", arguments];
                NSArray *effectsRaw = [arguments objectForKey:@"DarwinEqualizer"];
                if (!effectsRaw) {
                        result([FlutterError errorWithCode:@"ABNORMAL_PARAMETER" message:@"no DarwinEqualizer type found" details:nil]);
                        return;
                }
                
                NSDictionary *equalizerRaw = [effectsRaw filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(id  _Nullable evaluatedObject, NSDictionary<NSString *,id> * _Nullable bindings) {
                        return [[evaluatedObject objectForKey:@"type"] isEqualToString:@"DarwinEqualizer"];
                }]].firstObject;
                
                NSDictionary *parameters = [equalizerRaw objectForKey:@"parameters"];
                NSArray *rawBands = [parameters objectForKey:@"bands"];
                BOOL enabled = [[equalizerRaw objectForKey:@"enabled"] boolValue];
                
                NSArray *frequenciesAndBands = [rawBands filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(id  _Nullable evaluatedObject, NSDictionary<NSString *,id> * _Nullable bindings) {
                        return [evaluatedObject objectForKey:@"centerFrequency"] && [evaluatedObject objectForKey:@" Gain"];
                }]];
                
                NSArray *frequencies = [frequenciesAndBands valueForKeyPath:@"centerFrequency"];
                NSArray *bands = [frequenciesAndBands valueForKeyPath:@" Gain"];
                
                if (!audioEngine) {
                        [flutterSoundPlayer logDebug: @"Setting audio engine!"];
                        audioEngine = [[AVAudioEngine alloc] init];
                        
                        if (!eq) {
                                [flutterSoundPlayer logDebug: @"Setting Equalizer!"];
                                eq = [[AVAudioUnitEQ alloc] initWithNumberOfBands:(unsigned int)bands.count];
                                
                                for (int i = 0; i < rawBands.count; i++) {
                                        NSDictionary *band = rawBands[i];
                                        eq.bands[i].filterType = AVAudioUnitEQFilterTypeParametric;
                                        eq.bands[i].frequency = [[band objectForKey:@"centerFrequency"] floatValue];
                                        eq.bands[i].bandwidth = 1.0f;
                                        eq.bands[i]. Gain = EqualizerEffectData.GainFrom([[band objectForKey:@" Gain"] floatValue]);
                                        eq.bands[i].byPass = NO;
                                }
                                eq.bypass = !enabled;

                        }else{
                                [flutterSoundPlayer logDebug: @"Equalizer already set!"];
                        }
                } else {
                        [flutterSoundPlayer logDebug:@"Audio engine already set!"];
                }
        }


       -(void) startPlayerFromBuffer: (NSData*) dataBuffer
       {
                 [self feed: dataBuffer] ;
       }
        static int ready = 0;

       -(void)  startPlayerFromURL: (NSURL*) url codec: (t_CODEC)codec channels: (int)numChannels sampleRate: (long)sampleRate
       {
                assert(url == nil || url ==  (id)[NSNull null]);
                m_sampleRate = sampleRate;
                m_numChannels= numChannels;
                ready = 0;
       }


       -(long)  getDuration
       {
		return [self getPosition]; // It would be better if we add what is in the input buffers and not still played
       }

       -(long)  getPosition
       {
		double time ;
		if (mStartPauseTime >= 0) // In pause mode
			time =   mStartPauseTime - systemTime - mPauseTime ;
		else
			time = CACurrentMediaTime() - systemTime - mPauseTime;
		return (long)(time * 1000);
       }

       -(void)  stop
       {

                if (engine != nil)
                {
                        if (playerNode != nil)
                        {
                                [playerNode stop];
                                // Does not work !!! // [engine detachNode:  playerNode];
                                playerNode = nil;
                         }
                        [engine stop];
                        engine = nil;
                    
                        if (converter != nil)
                        {
                            converter = nil; // ARC will dealloc the converter (I hope ;-) )
                        }
                }
       }

        -(bool) play
        {
                [playerNode play];
                return true;

        }
       -(bool)  resume
       {
		if (mStartPauseTime >= 0)
			mPauseTime += CACurrentMediaTime() - mStartPauseTime;
		mStartPauseTime = -1;

		[playerNode play];
                return true;
       }

       -(bool)  pause
       {
		mStartPauseTime = CACurrentMediaTime();
		[playerNode pause];
                return true;
       }


       -(bool)  seek: (double) pos
       {
                return false;
       }

       -(int)  getStatus
       {
                if (engine == nil)
                        return PLAYER_IS_STOPPED;
                if (mStartPauseTime > 0)
                        return PLAYER_IS_PAUSED;
                if ( [playerNode isPlaying])
                        return PLAYER_IS_PLAYING;
                return PLAYER_IS_PLAYING; // ??? Not sure !!!
       }

        #define NB_BUFFERS 4
        - (int) feed: (NSData*)data
        {
                if (ready < NB_BUFFERS )
                {
                        int ln = (int)[data length];
                        int frameLn = ln/2;
                        int frameLength =  8*frameLn;// Two octets for a frame (Monophony, INT Linear 16)

                        playerFormat = [[AVAudioFormat alloc] initWithCommonFormat: AVAudioPCMFormatInt16 sampleRate: (double)m_sampleRate channels: m_numChannels interleaved: NO];

                        AVAudioPCMBuffer* thePCMInputBuffer =  [[AVAudioPCMBuffer alloc] initWithPCMFormat: playerFormat frameCapacity: frameLn];
                        memcpy((unsigned char*)(thePCMInputBuffer.int16ChannelData[0]), [data bytes], ln);
                        thePCMInputBuffer.frameLength = frameLn;
                        static bool hasData = true;
                        hasData = true;
                        AVAudioConverterInputBlock inputBlock = ^AVAudioBuffer*(AVAudioPacketCount inNumberOfPackets, AVAudioConverterInputStatus* outStatus)
                        {
                                *outStatus = hasData ? AVAudioConverterInputStatus_HaveData : AVAudioConverterInputStatus_NoDataNow;
                                hasData = false;
                                return thePCMInputBuffer;
                        };

                        AVAudioPCMBuffer* thePCMOutputBuffer = [[AVAudioPCMBuffer alloc] initWithPCMFormat: outputFormat frameCapacity: frameLength];
                        thePCMOutputBuffer.frameLength = 0;

                        if (converter == nil) 
                        {
                                converter = [[AVAudioConverter alloc]initFromFormat: playerFormat toFormat: outputFormat];
                        }

                        NSError* error;
                        [converter convertToBuffer: thePCMOutputBuffer error: &error withInputFromBlock: inputBlock];
                         // if (r == AVAudioConverterOutputStatus_HaveData || true)
                        {
                                ++ready ;
                                [playerNode scheduleBuffer: thePCMOutputBuffer  completionHandler:
                                ^(void)
                                {
                                        dispatch_async(dispatch_get_main_queue(),
                                        ^{
                                                --ready;
                                                assert(ready < NB_BUFFERS);
                                                if (self ->waitingBlock != nil)
                                                {
                                                        NSData* blk = self ->waitingBlock;
                                                        self ->waitingBlock = nil;
                                                        int ln = (int)[blk length];
                                                        int l = [self feed: blk]; // Recursion here
                                                        assert (l == ln);
                                                        [self ->flutterSoundPlayer needSomeFood: ln];
                                                }
                                        });

                                }];
                                return ln;
                        }
                } else
                {
                        assert (ready == NB_BUFFERS);
                        assert(waitingBlock == nil);
                        waitingBlock = data;
                        return 0;
                }
         }

-(bool)  setVolume: (double) volume fadeDuration: (NSTimeInterval)fadeDuration// TODO
{
        return true; // TODO
}

- (bool) setSpeed: (double) speed
{
        return true; // TODO
}


@end

// ---------------------------------------------------------------------------------------------------------------------------------------------------------------


@implementation AudioEngineFromMic
{
        FlautoPlayer* flutterSoundPlayer; // Owner
        AVAudioEngine* engine;
        AVAudioPlayerNode* playerNode;
        AVAudioFormat* playerFormat;
        AVAudioFormat* outputFormat;
        AVAudioOutputNode* outputNode;
        CFTimeInterval mStartPauseTime ; // The time when playback was paused
	CFTimeInterval systemTime ; //The time when  StartPlayer() ;
        double mPauseTime ; // The number of seconds during the total Pause mode
        NSData* waitingBlock;
        long m_sampleRate ;
        int  m_numChannels;
}

       - (AudioEngineFromMic*)init: (FlautoPlayer*)owner
       {
                flutterSoundPlayer = owner;
                waitingBlock = nil;
                engine = [[AVAudioEngine alloc] init];
                
                AVAudioInputNode* inputNode = [engine inputNode];
                outputNode = [engine outputNode];
                outputFormat = [outputNode inputFormatForBus: 0];
                
                [engine connect: inputNode to: outputNode format: outputFormat];
                return [super init];
       }
       

       -(void) startPlayerFromBuffer: (NSData*) dataBuffer
       {
       }
        static int ready2 = 0;

       -(long)  getDuration
       {
		return [self getPosition]; // It would be better if we add what is in the input buffers and not still played
       }

       -(long)  getPosition
       {
		double time ;
		if (mStartPauseTime >= 0) // In pause mode
			time =   mStartPauseTime - systemTime - mPauseTime ;
		else
			time = CACurrentMediaTime() - systemTime - mPauseTime;
		return (long)(time * 1000);
       }

       -(void)  startPlayerFromURL: (NSURL*) url codec: (t_CODEC)codec channels: (int)numChannels sampleRate: (long)sampleRate
       {
                assert(url == nil || url ==  (id)[NSNull null]);

                m_sampleRate = sampleRate;
                m_numChannels= numChannels;

                mPauseTime = 0.0; // Total number of seconds in pause mode
		mStartPauseTime = -1; // Not in paused mode
		systemTime = CACurrentMediaTime(); // The time when started
                ready2 = 0;
       }


       -(void)  stop
       {

                if (engine != nil)
                {
                         [engine stop];
                        engine = nil;
                }
       }

        -(bool) play
        {
                bool b = [engine startAndReturnError: nil];
                if (!b)
                {
                        [flutterSoundPlayer logDebug: @"Cannot start the audio engine"];
                }
                return b;
        }

       -(bool)  resume
       {
                return false;
       }

       -(bool)  pause
       {
                return false;
       }


       -(bool)  seek: (double) pos
       {
                return false;
       }

       -(int)  getStatus
       {
                if (engine == nil)
                        return PLAYER_IS_STOPPED;
                return PLAYER_IS_PLAYING; // ??? Not sure !!!
       }


        -(bool)  setVolume: (double) volume fadeDuration: (NSTimeInterval) fadeDuration // TODO
        {
                return true; // TODO
        }

        -(bool)  setSpeed: (double) speed // TODO
        {
                return true; // TODO
        }

      - (int) feed: (NSData*)data
       {
        return 0;
       }


//-------------------------------------------------------------------------------------------------------------------------------------------


// convert this to objective-c:
// struct EqualizerEffectData: EffectData, Codable {
//     let type: EffectType
//     let enabled: Bool
//     let parameters: ParamsEqualizerData

//     static func fromJson(_ map: [String: Any]) -> EqualizerEffectData {
//         return try! JSONDecoder().decode(EqualizerEffectData.self, from: JSONSerialization.data(withJSONObject: map))
//     }

//     static func effectFrom(_ map: [String: Any]) throws -> EffectData {
//         let type = map["type"] as! String
//         switch type {
//         case EffectType.darwinEqualizer.rawValue:
//             return EqualizerEffectData.fromJson(map)
//         default:
//             throw NotSupportedError(value: type, "When decoding effect")
//         }
//     }
    
//     static func gainFrom(_ value: Float) -> Float {
//         // Equalize the level between iOS and android
//         return value * 2.8
//     }
// }

@implementation EqualizerEffectData
{
        // convert this to objective-c:
        //     let type: EffectType
        //     let enabled: Bool
        //     let parameters: ParamsEqualizerData

        EffectType: type;
        bool enabled;
        ParamsEqualizerData parameters;
}

        // convert this to objective-c:
        //     static func fromJson(_ map: [String: Any]) -> EqualizerEffectData {
        //         return try! JSONDecoder().decode(EqualizerEffectData.self, from: JSONSerialization.data(withJSONObject: map))
        //     }

        -(EqualizerEffectData*) fromJson: (NSDictionary*) map
        {
                return JSONDecoder().decode(EqualizerEffectData.self, from: JSONSerialization.data(withJSONObject: map));
        }



        // convert this to objective-c:
        //     static func effectFrom(_ map: [String: Any]) throws -> EffectData {
        //         let type = map["type"] as! String
        //         switch type {
        //         case EffectType.darwinEqualizer.rawValue:
        //             return EqualizerEffectData.fromJson(map)
        //         default:
        //             throw NotSupportedError(value: type, "When decoding effect")
        //         }
        //     }

        -(EffectData*) effectFrom: (NSDictionary*) map
        {
                NSString* type = map[@"type"];
                switch (type)
                {
                        case EffectType.darwinEqualizer.rawValue:
                                return [self fromJson: map];
                        default:
                                throw NotSupportedError(value: type, "When decoding effect");
                }
        }

        // convert this to objective-c:
        //     static func gainFrom(_ value: Float) -> Float {
        //         // Equalize the level between iOS and android
        //         return value * 2.8
        //     }

        -(float) gainFrom: (float) value
        {
                // Equalize the level between iOS and android
                return value * 2.8;
        }



@end

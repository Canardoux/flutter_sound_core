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

       - (AudioEngine*)init: (FlautoPlayer*)owner eqParams: (NSDictionary*) params
       {
            flutterSoundPlayer = owner;
            waitingBlock = nil;
            engine = [[AVAudioEngine alloc] init];
            outputNode = [engine outputNode];
            playerNode = [[AVAudioPlayerNode alloc] init];
           
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
           
           eq = [self setEqualizer: params];
           
           if(eq){
               [flutterSoundPlayer logDebug: @"EQ found! Attach it"];
               [engine attachNode: eq];
           } else {
               [flutterSoundPlayer logDebug: @"EQ NOT FOUND!!!"];
           }
           
           [engine attachNode: playerNode];
           AVAudioMixerNode *mixerNode = [engine mainMixerNode];
//           AVAudioFormat * format = [[engine mainMixerNode] outputFormatForBus:0];
       
            if(eq){
                [flutterSoundPlayer logDebug: @"EQ found! Connect it"];
                [engine connect: playerNode to: eq format: outputFormat];
                [engine connect: eq to: mixerNode format: outputFormat];
            } else {
                [engine connect: playerNode to: outputNode format: outputFormat];
            }
           
           [engine prepare];

            bool b = [engine startAndReturnError: nil];
            if (!b)
            {
                NSLog(@"Cannot start the audio engine!");
                [flutterSoundPlayer logDebug: @"Cannot start the audio engine"];
            }

            mPauseTime = 0.0; // Total number of seconds in pause mode
            mStartPauseTime = -1; // Not in paused mode
            systemTime = CACurrentMediaTime(); // The time when started
           return [super init];
       }

       - (void) enableEqualizer:(bool) enabled
       {
            if (eq == nil)
            {
                [flutterSoundPlayer logDebug: @"Cannot enable the equalizer because it is not initialized"];
                return;
            }
           
           [engine prepare];
           eq.bypass = !enabled;
           
           // Start the engine.
           NSError *error;
           [engine startAndReturnError:&error];
           if (error) {
               NSLog(@"error:%@", error);
           }
       }

       - (void) setEqualizerBandGain: (int) bandIndex gain: (float) gain
       {
           if (eq == nil)
           {
               [flutterSoundPlayer logDebug: @"Cannot set the equalizer band gain because the equalizer is not initialized"];
               NSLog(@"Cannot set the equalizer band gain because the equalizer is not initialized");
               return;
           }
           if (bandIndex < 0 || bandIndex >= eq.bands.count)
           {
               [flutterSoundPlayer logDebug: @"Cannot set the equalizer band gain because the band index is out of range"];
               NSLog(@"Cannot set the equalizer band gain because the band index is out of range");
               return;
           }
           
           [engine prepare];
           
//           NSLog(@"Received (flutter): band %d: to: %f ",bandIndex, gain);
           
//           NSLog(@"Setting gain for band: %d to %f", bandIndex, [self gainFrom: gain]);
           
           AVAudioUnitEQFilterParameters* band = eq.bands[bandIndex];
           band.gain = [self gainFrom: gain];
           
           // Start the engine.
           NSError *error;
           [engine startAndReturnError:&error];
           if (error) {
               NSLog(@"error:%@", error);
           }
       }

       - (AVAudioUnitEQ *) setEqualizer:(NSDictionary*) arguments; {
           [flutterSoundPlayer logDebug:@"Initialize darwin EQ!"];
           
           AVAudioUnitEQ* _eq;
           
           // Collect all DarwinEqulizer related
           NSArray *effectsRaw = [arguments objectForKey:@"DarwinEqualizer"];
           if (!effectsRaw) {
               [flutterSoundPlayer logDebug:@"no DarwinEqualizer type found"];
               throw @"no DarwinEqualizer type found";
            }
           
           //
           NSDictionary *equalizerRaw = [effectsRaw filteredArrayUsingPredicate:[NSPredicate predicateWithBlock:^BOOL(id  _Nullable evaluatedObject, NSDictionary<NSString *,id> * _Nullable bindings) {
                    return [[evaluatedObject objectForKey:@"type"] isEqualToString:@"DarwinEqualizer"];
           }]].firstObject;
           
           // Collect parameters
           NSDictionary *parameters = [equalizerRaw objectForKey:@"parameters"];
           // Collect bands
           NSArray *rawBands = [parameters objectForKey:@"bands"];
           // Get enabled status
           BOOL enabled = [[equalizerRaw objectForKey:@"enabled"] boolValue];
                
           [flutterSoundPlayer logDebug: @"Setting Equalizer!"];
           
           // Init equalizer
           _eq = [[AVAudioUnitEQ alloc] initWithNumberOfBands:(unsigned int)rawBands.count + 1];
           [_eq setBypass : !enabled];
           [_eq setGlobalGain : 1];
//           // Set bands
           for (int i = 0; i < rawBands.count; i++) {
               NSDictionary *band = rawBands[i];
               _eq.bands[i].frequency = [[band objectForKey:@"centerFrequency"] floatValue];
//               _eq.bands[i].bandwidth = 1.0f;
               _eq.bands[i].gain = [self gainFrom: ([[band objectForKey:@" Gain"] floatValue])];
               _eq.bands[i].bypass = false;
               _eq.bands[i].filterType = AVAudioUnitEQFilterTypeParametric;
           }
           
//           //Band pass filter
//           AVAudioUnitEQFilterParameters *bandPassFilter;
//           bandPassFilter = _eq.bands[(unsigned int)rawBands.count];
//           bandPassFilter.frequency = 2000;
////           bandPassFilter.bandwidth = 2.0f;
//           bandPassFilter.gain = -60;
//           bandPassFilter.bypass = false;
//           bandPassFilter.filterType = AVAudioUnitEQFilterTypeLowPass;
           
           return _eq;
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
                    if(eq != nil){
                        eq = nil;
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
                        int frameLn = ln/4;
                        int frameLength =  frameLn;// Two octets for a frame (Monophony, INT Linear 16)

                        AVAudioChannelLayout *chLayout = [[AVAudioChannelLayout alloc] initWithLayoutTag:kAudioChannelLayoutTag_Stereo];
                        playerFormat = [[AVAudioFormat alloc] 
                                initWithCommonFormat: AVAudioPCMFormatInt16
                                sampleRate: (double)m_sampleRate
                                // channels: m_numChannels
                                interleaved: YES
                                channelLayout: chLayout];

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


        - (float) gainFrom:(float) value
        {
            //Equalize the level between iOS and Android
            return value * 2.8;
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

- (void)enableEqualizer:(bool)enabled {
    NSLog(@"enableEqualizer not implemented (MIC)");
}


- (void)setEqualizerBandGain:(int)bandIndex gain:(float)gain {
    NSLog(@"setEqualizerBandGain not implemented (MIC)");
}




//-------------------------------------------------------------------------------------------------------------------------------------------
@end

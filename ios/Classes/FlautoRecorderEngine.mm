//
//  AudioRecorder.m
//  flutter_sound
//
//  Created by larpoux on 02/05/2020.
//
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



#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

#import "Flauto.h"
#import "FlautoRecorderEngine.h"


//-------------------------------------------------------------------------------------------------------------------------------------------



/* ctor */ AudioRecorderEngine::AudioRecorderEngine(t_CODEC coder, NSString* path, NSMutableDictionary* audioSettings, long bufferSize, bool enableVoiceProcessing, FlautoRecorder* owner )
{
 
         flautoRecorder = owner;
         engine = [[AVAudioEngine alloc] init];
         dateCumul = 0;
         previousTS = 0;
         status = 0;
        
   
        
        AVAudioInputNode* inputNode = [engine inputNode];
        AVAudioFormat* inputFormat = [inputNode outputFormatForBus: 0];
        NSNumber* nbChannels = audioSettings [AVNumberOfChannelsKey];
        NSNumber* sampleRate = audioSettings [AVSampleRateKey];
        int channelCount = [inputFormat channelCount];
        //int inputSampleRate = [inputFormat sampleRate];
        //AVAudioCommonFormat inputCommonFormat = [inputFormat commonFormat];
        //bool interleaved = [inputFormat isInterleaved];
        AVAudioCommonFormat cf = [inputFormat commonFormat];

        NSFileManager* fileManager = [NSFileManager defaultManager];
        NSURL* fileURL = nil;
        if (path != nil && path != (id)[NSNull null])
        {
                if (channelCount > 1)
                {
                    [flautoRecorder logDebug: @"Invalid channelCount >1 not yet supported"];
                    [NSException raise: @"Invalid channelCount" format:@"channelCount == %d is invalid", channelCount];
                }
                [fileManager removeItemAtPath:path error:nil];
                [fileManager createFileAtPath: path contents:nil attributes:nil];
                fileURL = [[NSURL alloc] initFileURLWithPath: path];
                fileHandle = [NSFileHandle fileHandleForWritingAtPath: path];
        } else
        {
                fileHandle = nil;
        }

        if (coder == pcmFloat32 && cf == AVAudioPCMFormatFloat32)
        {
                
                [inputNode installTapOnBus: 0 bufferSize: (int)bufferSize format: nil block:
                ^(AVAudioPCMBuffer* buf, AVAudioTime* when)
                {
                // __'__buf' contains captured audio from the node at time 'when'
                        int ln = [buf frameLength];
                        float* const* data = [buf floatChannelData];
                        if (ln > 0)
                        {
                            if (fileHandle != nil)
                            {
                                    NSData* b = [[NSData alloc] initWithBytes: data[0] length: ln * 4 ];
                                    [fileHandle writeData: b];
                            } else
                            {
                                NSMutableArray* d = [NSMutableArray array];
                                for (int channel = 0; channel < channelCount; ++channel)
                                {
                                        NSData const* b = [[NSData alloc] initWithBytes: data[channel] length: ln * 4 ];
                                        [d addObject: b];
                                }
                                dispatch_async(dispatch_get_main_queue(),
                               ^{
                                    if (flautoRecorder == nil || getStatus() == 0) // something bad. Ignore.
                                    {
                                            return;
                                    }
                                    [flautoRecorder  recordingDataFloat32: d];
                                });

                            }
 
                        }
                        
                }];
                return;
         } 

         AVAudioFormat* inpFormat =  [ [AVAudioFormat alloc] initWithCommonFormat: AVAudioPCMFormatFloat32
                                      sampleRate: 48000 // Must be fixed because of iOS bug !
                                                                        channels: channelCount interleaved: false];

         AVAudioFormat* recordingFormat = [[AVAudioFormat alloc] initWithCommonFormat: AVAudioPCMFormatInt16 sampleRate: sampleRate.doubleValue channels: (unsigned int)(nbChannels.unsignedIntegerValue) interleaved: YES];
         AVAudioConverter* converter = [[AVAudioConverter alloc]initFromFormat: inpFormat toFormat: recordingFormat];
         //AVAudioFormat *format = [inputNode outputFormatForBus: 0];

         [inputNode installTapOnBus: 0 bufferSize: (int)bufferSize format: nil block:
         ^(AVAudioPCMBuffer* _Nonnull buffer, AVAudioTime* _Nonnull when)
         {
                         inputStatus = AVAudioConverterInputStatus_HaveData ;
                         AVAudioPCMBuffer* convertedBuffer = [[AVAudioPCMBuffer alloc]initWithPCMFormat: recordingFormat frameCapacity: [buffer frameCapacity]];
                                                  
                         AVAudioConverterInputBlock inputBlock =
                         ^AVAudioBuffer*(AVAudioPacketCount inNumberOfPackets, AVAudioConverterInputStatus *outStatus)
                         {
                                 *outStatus = inputStatus;
                                 inputStatus =  AVAudioConverterInputStatus_NoDataNow;
                                 return buffer;
                         };
                         NSError* error;
                         BOOL r = [converter convertToBuffer: convertedBuffer error: &error withInputFromBlock: inputBlock];
                         if (!r)
                         {
                         }
                         
                         int n = [convertedBuffer frameLength];
                         int16_t *const  bb = [convertedBuffer int16ChannelData][0];
                         NSData* b = [[NSData alloc] initWithBytes: bb length: n * 2 ];
                         if (n > 0)
                         {
                                 if (fileHandle != nil)
                                 {
                                         [fileHandle writeData: b];
                                 } else
                                 {
                                         dispatch_async(dispatch_get_main_queue(),
                                        ^{
                                                 if (flautoRecorder == nil || getStatus() == 0) // something bad
                                                 {
                                                         return;
                                                 }
                                                 [flautoRecorder  recordingData: b];
                                         });
                                 }
                                 
                                 int16_t* pt = [convertedBuffer int16ChannelData][0];
                                 for (int i = 0; i < [buffer frameLength]; ++pt, ++i)
                                 {
                                         short curSample = *pt;
                                         if ( curSample > maxAmplitude )
                                         {
                                                 maxAmplitude = curSample;
                                         }
                                         
                                 }
                         }
         }];
     
}

int AudioRecorderEngine::startRecorder()
{
        [engine startAndReturnError: nil];
        previousTS = CACurrentMediaTime() * 1000;
        status = 2;
        return status;
}

void AudioRecorderEngine::stopRecorder()
{
        [engine stop];
        [fileHandle closeFile];
        if (previousTS != 0)
        {
                dateCumul += CACurrentMediaTime() * 1000 - previousTS;
                previousTS = 0;
        }
        status = 0;
        engine = nil;
}

void AudioRecorderEngine::resumeRecorder()
{
        [engine startAndReturnError: nil];
        previousTS = CACurrentMediaTime() * 1000;
        status = 2;
 
}

void AudioRecorderEngine::pauseRecorder()
{
        [engine pause];
        if (previousTS != 0)
        {
                dateCumul += CACurrentMediaTime() * 1000 - previousTS;
                previousTS = 0;
        }
        status = 1;
 
}

NSNumber* AudioRecorderEngine::recorderProgress()
{
        long r = dateCumul;
        if (previousTS != 0)
        {
                r += CACurrentMediaTime() * 1000 - previousTS;
        }
        return [NSNumber numberWithInt: (int)r];
}

NSNumber* AudioRecorderEngine::dbPeakProgress()
{
        double max = (double)maxAmplitude;
        maxAmplitude = 0;
        if (max == 0.0)
        {
                // if the microphone is off we get 0 for the amplitude which causes
                // db to be infinite.
                return [NSNumber numberWithDouble: 0.0];
        }
        

        // Calculate db based on the following article.
        // https://stackoverflow.com/questions/10655703/what-does-androids-getmaxamplitude-function-for-the-mediarecorder-actually-gi
        //
        double ref_pressure = 51805.5336;
        double p = max / ref_pressure;
        double p0 = 0.0002;
        double l = log10(p / p0);

        double db = 20.0 * l;

        return [NSNumber numberWithDouble: db];
}


int AudioRecorderEngine::getStatus()
{
     return status;
}



//-----------------------------------------------------------------------------------------------------------------------------------------
/* ctor */ avAudioRec::avAudioRec( t_CODEC codec, NSString* path, NSMutableDictionary *audioSettings, FlautoRecorder* owner)
{
        flautoRecorder = owner;
        isPaused = false;

        NSURL *audioFileURL;
        {
                audioFileURL = [NSURL fileURLWithPath: path];
        }

        audioRecorder = [[AVAudioRecorder alloc]
                        initWithURL:audioFileURL
                        settings:audioSettings
                        error:nil];
}

/* dtor */ avAudioRec::~avAudioRec()
{
        [audioRecorder stop];
        isPaused = false;
}

int avAudioRec::startRecorder()
{
          [audioRecorder setDelegate: flautoRecorder];
          [audioRecorder record];
          [audioRecorder setMeteringEnabled: YES];
          isPaused = false;
          if ( [audioRecorder isRecording] )
             return 2;
          return 0;
}

void avAudioRec::stopRecorder()
{
        isPaused = false;
        [audioRecorder stop];
}

void avAudioRec::resumeRecorder()
{
        [audioRecorder record];
        isPaused = false;
}

void avAudioRec::pauseRecorder()
{
        [audioRecorder pause];
        isPaused = true;

}

NSNumber* avAudioRec::recorderProgress()
{
        NSNumber* duration =    [NSNumber numberWithLong: (long)(audioRecorder.currentTime * 1000 )];

        
        [audioRecorder updateMeters];
        return duration;
}

NSNumber* avAudioRec::dbPeakProgress()
{
        NSNumber* normalizedPeakLevel = [NSNumber numberWithDouble:MIN(pow(10.0, [audioRecorder peakPowerForChannel:0] / 20.0) * 160.0, 160.0)];
        return normalizedPeakLevel;

}

int avAudioRec::getStatus()
{
     if ( [audioRecorder isRecording] )
        return 2;
     else if (isPaused)
        return 1;
     return 0;
}



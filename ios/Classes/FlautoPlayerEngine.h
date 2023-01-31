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

#ifndef PlayerEngine_h
#define PlayerEngine_h


#include "Flauto.h"
#import <AVFoundation/AVFoundation.h>

@protocol FlautoPlayerEngineInterface <NSObject>

    - (void) startPlayerFromBuffer:  (NSData*)data ;
    - (void) startPlayerFromURL: (NSURL*) url codec: (t_CODEC)codec channels: (int)numChannels sampleRate: (long)sampleRate ;
    - (long) getDuration;
    - (long) getPosition;
    - (void) stop;
    - (bool) play;
    - (bool) resume;
    - (bool) pause;
    - (bool) setVolume: (double) volume fadeDuration: (NSTimeInterval)fadeDuration; // Volume is between 0.0 and 1.0
    - (bool) setSpeed: (double) speed ; // Speed is between 0.0 and 1.0 to go slower
    - (bool) seek: (double) pos;
    - (t_PLAYER_STATE) getStatus;
    - (int) feed: (NSData*)data;

@end

@interface AudioPlayerFlauto : NSObject  <FlautoPlayerEngineInterface>
{
}
    - (AudioPlayerFlauto*) init: (NSObject*)owner ;// FlutterSoundPlayer*

    - (void) startPlayerFromBuffer:  (NSData*)data ;
    - (void) startPlayerFromURL: (NSURL*) url codec: (t_CODEC)codec channels: (int)numChannels sampleRate: (long)sampleRate ;
    - (void) stop;
    - (bool) play;
    - (bool) resume;
    - (bool) pause;
    - (bool) setVolume: (double) volume fadeDuration: (NSTimeInterval)duration ;// Volume is between 0.0 and 1.0
    - (bool) setSpeed: (double) speed ;// Volume is between 0.0 and 1.0
    - (bool) seek: (double) pos;
    - (t_PLAYER_STATE) getStatus;
    - (AVAudioPlayer*) getAudioPlayer;
    - (void) setAudioPlayer: (AVAudioPlayer*)thePlayer;
    - (int) feed: (NSData*)data;
    - (AVAudioUnitEQ *) setEqualizer:(NSDictionary*) arguments;

@end


@interface AudioEngine  : NSObject <FlautoPlayerEngineInterface>
{
        // TODO FlutterSoundPlayer* flutterSoundPlayer; // Owner
}
    - (AudioEngine*)init: (NSObject*)owner eqParams: (NSDictionary*) params;

    - (void) startPlayerFromBuffer:  (NSData*)data ;
    - (void) startPlayerFromURL: (NSURL*) url codec: (t_CODEC)codec channels: (int)numChannels sampleRate: (long)sampleRate ;
    - (bool) play;
    - (void) stop;
    - (bool) resume;
    - (bool) pause;
    - (bool) setVolume: (double) volume  fadeDuration:(NSTimeInterval)duration ;
    - (bool) setSpeed: (double) speed ;
    - (bool) seek: (double) pos;
    - (int)  getStatus;
    - (int) feed: (NSData*)data;
    - (float)gainFrom:(float)value;

@end


@interface AudioEngineFromMic  : NSObject <FlautoPlayerEngineInterface>
{
        // TODO FlutterSoundPlayer* flutterSoundPlayer; // Owner
}
    - (AudioEngineFromMic*) init: (NSObject*)owner; // FlutterSoundPlayer*

    - (void) startPlayerFromBuffer:  (NSData*)data ;
    - (void) startPlayerFromURL: (NSURL*) url codec: (t_CODEC)codec channels: (int)numChannels sampleRate: (long)sampleRate ;
    - (void) stop;
    - (bool) play;
    - (bool) resume;
    - (bool) pause;
    - (bool) setVolume: (double) volume  fadeDuration:(NSTimeInterval)duration ;
    - (bool) setSpeed: (double) speed;
    - (bool) seek: (double) pos;
    - (int) getStatus;
    - (int) feed: (NSData*)data;

@end

//#pragma mark - Enum EffectType
//typedef NS_ENUM(NSUInteger, EffectType) {
//        EffectTypeDarwinEqualizer, NotImplemented
//    };
//
//NSString *NSStringFromEffectType(EffectType type) {
//    switch (type) {
//        case EffectTypeDarwinEqualizer:
//            return @"DarwinEqualizer";
//        default:
//            return @"";
//    }
//}
//EffectType EffectTypeFromNSString(NSString *string) {
//    if ([string isEqualToString:@"DarwinEqualizer"]) {
//        return EffectTypeDarwinEqualizer;
//    }
//    return NotImplemented;
//}
//
//#pragma mark - Protocol EffectData
//@protocol EffectData <NSObject>
//    @property (nonatomic, assign, readonly) EffectType type;
//@end
//
//#pragma mark - Struct BandEqualizerData
//@interface BandEqualizerData : NSObject <NSCoding, NSSecureCoding>
//    @property (nonatomic, assign) NSUInteger index;
//    @property (nonatomic, assign) float centerFrequency;
//    @property (nonatomic, assign) float gain;
//@end
//
//#pragma mark - Struct ParamsEqualizerData
//@interface ParamsEqualizerData : NSObject <NSCoding, NSSecureCoding>
//    @property (nonatomic, strong) NSArray<BandEqualizerData *> *bands;
//@end
//
//#pragma mark - Struct EqualizerEffectData
//@interface EqualizerEffectData : NSObject <EffectData>
//    @property (nonatomic, assign) EffectType type;
//    @property (nonatomic, assign) BOOL enabled;
//    @property (nonatomic, strong) ParamsEqualizerData *parameters;
//    - (instancetype)fromJson:(NSDictionary *)map;
//    - (id<EffectData>)effectFrom:(NSDictionary *)map error:(NSError **)error;
//@end


#endif /* PlayerEngine_h */

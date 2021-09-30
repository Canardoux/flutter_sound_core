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


@interface FlautoTrack : NSObject
{
    NSString* path;
    NSString* title;
    NSString* author;
    NSString* albumArtUrl;
    NSString* albumArtAsset;
    NSData* dataBuffer;
}

@property(nonatomic, retain) NSString* _Nullable path;
@property(nonatomic, retain) NSString* _Nullable title;
@property(nonatomic, retain) NSString* _Nullable author;
@property(nonatomic, retain) NSString* _Nullable albumArtUrl;
@property(nonatomic, retain) NSString* _Nullable albumArtAsset;
@property(nonatomic, retain) NSString* _Nullable albumArtFile;
@property(nonatomic, retain) NSData* _Nullable dataBuffer;

- (_Nullable id)initFromJson:(NSString* _Nullable )jsonString;
- (_Nullable id)initFromDictionary:(NSDictionary* _Nullable )jsonData;
- (bool)isUsingPath;

@end

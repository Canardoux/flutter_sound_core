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
#import "FlautoTrack.h"

@implementation FlautoTrack

@synthesize path;
@synthesize title;
@synthesize author;
@synthesize albumArtUrl;
@synthesize albumArtAsset;
@synthesize albumArtFile;
@synthesize dataBuffer;

// Returns true if the audio file is stored as a path represented by a string, false if
// it is stored as a buffer.
-(bool) isUsingPath {
    return [path class] != [NSNull class];
}

-(id) initFromJson:(NSString*) jsonString {
    NSData* jsonData = [jsonString dataUsingEncoding:NSUTF8StringEncoding];

    NSError *error = nil;
    NSDictionary *responseObj = [NSJSONSerialization
                                 JSONObjectWithData:jsonData
                                 options:0
                                 error:&error];

    if(! error) {
        NSString *pathString = [responseObj objectForKey: @"path"];
        path = pathString;

        NSString *titleString = [responseObj objectForKey :@"title"];
        title = titleString;

        NSString *authorString = [responseObj objectForKey: @"author"];
        author = authorString;

        NSString *albumArtUrlString = [responseObj objectForKey: @"albumArtUrl"];
        albumArtUrl = albumArtUrlString;

        NSString *albumArtAssetString = [responseObj objectForKey: @"albumArtAsset"];
        albumArtAsset = albumArtAssetString;
        
        NSString *albumArtFileString = [responseObj objectForKey: @"albumArtFile"];
        albumArtFile = albumArtFileString;


        NSData* dataBufferJson = [responseObj objectForKey: @"dataBuffer"];
        dataBuffer = dataBufferJson;
    } else {
        NSLog(@"Error in parsing JSON");
        return nil;
    }

    return self;
}

-(id) initFromDictionary:(NSDictionary*) jsonData {
    NSString *pathString = [jsonData objectForKey: @"path"];
    path = pathString;

    NSString *titleString = [jsonData objectForKey: @"title"];
    title = titleString;

    NSString *authorString = [jsonData objectForKey: @"author"];
    author = authorString;

    NSString *albumArtUrlString = [jsonData objectForKey: @"albumArtUrl"];
    albumArtUrl = albumArtUrlString;

    NSString *albumArtAssetString = [jsonData objectForKey: @"albumArtAsset"];
    albumArtAsset = albumArtAssetString;
    
    NSString *albumArtFileString = [jsonData objectForKey: @"albumArtFile"];
    albumArtFile = albumArtFileString;


    NSData* dataBufferJson = [jsonData objectForKey: @"dataBuffer"];
    dataBuffer = dataBufferJson;

    return self;
}

@end

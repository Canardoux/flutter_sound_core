package xyz.canardoux.TauEngine;
/*
 * Copyright 2018, 2019, 2020, 2021 Canardoux.
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

import android.app.Activity;
import android.content.Context;

import java.io.File;
//import androidx.annotation.NonNull;



public class Flauto
{

	public enum t_CODEC
	{
		defaultCodec,
		aacADTS,
		opusOGG,
		opusCAF, // Apple encapsulates its bits in its own special envelope : .caf instead of a regular ogg/opus (.opus). This is completely stupid, this is Apple.
		mp3,
		vorbisOGG,
		pcm16,
		pcm16WAV,
		pcm16AIFF,
		pcm16CAF,
		flac,
		aacMP4,
		amrNB,
		amrWB,
		pcm8,
		pcmFloat32,
		pcmWebM,
		opusWebM,
		vorbisWebM,
	}



	public enum t_PLAYER_STATE
	{
		PLAYER_IS_STOPPED,
		PLAYER_IS_PLAYING,
		PLAYER_IS_PAUSED
	}


	public enum t_RECORDER_STATE
	{
		RECORDER_IS_STOPPED,
		RECORDER_IS_PAUSED,
		RECORDER_IS_RECORDING,
	}





	public enum t_AUDIO_SOURCE
	{
		defaultSource,
		microphone,
		voiceDownlink, // (if someone can explain me what it is, I will be grateful ;-) )
		camCorder,
		remote_submix,
		unprocessed,
		voice_call,
		voice_communication,
		voice_performance,
		voice_recognition,
		voiceUpLink,
		bluetoothHFP,
		headsetMic,
		lineIn
	}



	public enum t_LOG_LEVEL
	{
				VERBOSE,
				DBG,
				INFO,
				WARNING,
				ERROR,
				WTF,
				NOTHING,
	}




	public static Activity androidActivity;
	public static Context androidContext;

	public static String temporayFile(String radical)
	{
		File outputDir = Flauto.androidContext.getCacheDir(); // context being the Activity pointer
		return outputDir + "/" + radical;

	}

	public static String getPath(String path)
	{
		if (path == null)
			return null;
		boolean isFound = path.contains("/");
		if (!isFound)
		{
			path = temporayFile(path);
		}
		return path;
	}

}

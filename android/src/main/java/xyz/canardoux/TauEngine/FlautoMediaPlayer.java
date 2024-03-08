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

import android.media.MediaPlayer;

//-------------------------------------------------------------------------------------------------------------



class FlautoMediaPlayer extends FlautoPlayerEngineInterface
{
	MediaPlayer mediaPlayer = null;
	FlautoPlayer flautoPlayer;

	void _startPlayer(String path, int sampleRate, int numChannels, int blockSize,  boolean enableVoiceProcessing, FlautoPlayer theSession) throws Exception
 	{
 		mediaPlayer = new MediaPlayer();
		if (path == null)
		{
			throw new Exception("path is NULL");
		}
		this.flautoPlayer = theSession;
		mediaPlayer.setDataSource(path);
		final String pathFile = path;
		mediaPlayer.setOnPreparedListener(mp -> {flautoPlayer.play(); flautoPlayer.onPrepared();});
		mediaPlayer.setOnCompletionListener(mp -> flautoPlayer.onCompletion());
		mediaPlayer.setOnErrorListener(flautoPlayer);
		mediaPlayer.prepare(); // Maybe too early. Should be after start()
	}

	void _play()
	{
		mediaPlayer.start();
	}

	int feed(byte[] data) throws Exception
	{
		throw new Exception("Cannot feed a Media Player");
	}

	void _setVolume(double volume)
	{
		float v = (float)volume;
		mediaPlayer.setVolume ( v, v );
	}

	void _setSpeed(double speed)
	{
		float v = (float)speed;
	}

	void _stop() {
		if (mediaPlayer == null)
		{
			return;
		}

		try
		{
			mediaPlayer.stop();
		} catch (Exception e)
		{
		}

		try
		{
			mediaPlayer.reset();
		} catch (Exception e)
		{
		}

		try
		{
			mediaPlayer.release();
		} catch (Exception e)
		{
		}
		mediaPlayer = null;

	}

	void _finish() { // NO-OP
	}



	void _pausePlayer() throws Exception {
		if (mediaPlayer == null) {
			throw new Exception("pausePlayer()");
		}
		mediaPlayer.pause();
	}

	void _resumePlayer() throws Exception {
		if (mediaPlayer == null) {
			throw new Exception("resumePlayer");
		}

		if (mediaPlayer.isPlaying()) {
			throw new Exception("resumePlayer");
		}
		// Is it really good ? // mediaPlayer.seekTo ( mediaPlayer.getCurrentPosition () );
		mediaPlayer.start();
	}

	void _seekTo(long millisec)
	{
		mediaPlayer.seekTo ( (int)millisec );
	}

	boolean _isPlaying()
	{
		return mediaPlayer.isPlaying ();
	}

	long _getDuration()
	{
		return mediaPlayer.getDuration();
	}

	long _getCurrentPosition()
	{
		return mediaPlayer.getCurrentPosition();
	}
}

//-------------------------------------------------------------------------------------------------------------

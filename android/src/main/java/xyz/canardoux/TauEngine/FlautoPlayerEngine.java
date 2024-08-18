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

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.SystemClock;
import java.lang.Thread;

//-------------------------------------------------------------------------------------------------------------

class FlautoPlayerEngine extends FlautoPlayerEngineInterface {
	AudioTrack audioTrack = null;
	int sessionId = 0;
	long mPauseTime = 0;
	long mStartPauseTime = -1;
	long systemTime = 0;
	WriteBlockThread blockThread = null;
	FlautoPlayer mSession = null;

	class WriteBlockThread extends Thread {
		byte[] mData = null;

		/* ctor */ WriteBlockThread(byte[] data) {
			mData = data;
		}

		public void run() {
			int ln = mData.length;
			int total = 0;
			int written = 0;
			while (audioTrack != null && ln > 0) { // Loop as long as there is data to push to the device
				try {
					if (Build.VERSION.SDK_INT >= 23) {
						written = audioTrack.write(mData, 0, ln, AudioTrack.WRITE_BLOCKING);
					} else {
						written = audioTrack.write(mData, 0, mData.length);
					}
					if (written > 0) {
						ln -= written;
						total += written;
					}
				} catch (Exception e) {
					System.out.println(e.toString());
					return;
				}
				if (ln > 0) {
					try {
						this.sleep(100); // Wait because the device is slow to accept our data
								// could be a this.yield()
					} catch (InterruptedException e) {

					}
				}
			}
			if (total < 0)
				throw new RuntimeException();

			mSession.needSomeFood(total);
			// if (ln == 0)
			// mSession.onCompletion();
			blockThread = null;

		}
	}

	/* ctor */ FlautoPlayerEngine() throws Exception {
		if (Build.VERSION.SDK_INT >= 21) {

			AudioManager audioManager = (AudioManager) Flauto.androidContext
					.getSystemService(Context.AUDIO_SERVICE);
			sessionId = audioManager.generateAudioSessionId();
		} else {
			throw new Exception("Need SDK 21");
		}
	}

	void _startPlayer(
			String path,
			int sampleRate,
			int numChannels,
			int bufferSize,
			boolean enableVoiceProcessing, // Not used on Android
			FlautoPlayer theSession) throws Exception {
		if (Build.VERSION.SDK_INT >= 21) {
			mSession = theSession;
			AudioAttributes attributes = new AudioAttributes.Builder()
					.setLegacyStreamType(AudioManager.STREAM_MUSIC)
					.setUsage(AudioAttributes.USAGE_MEDIA)
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.build();

			AudioFormat format = new AudioFormat.Builder()
					.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
					.setSampleRate(sampleRate)
					.setChannelMask(numChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO
							: AudioFormat.CHANNEL_OUT_STEREO)
					.build();
			audioTrack = new AudioTrack(attributes, format, bufferSize, AudioTrack.MODE_STREAM, sessionId);
			mPauseTime = 0;
			mStartPauseTime = -1;
			systemTime = SystemClock.elapsedRealtime();

			theSession.onPrepared(); // Maybe too early ??? Should be after _play()
		} else {
			throw new Exception("Need SDK 21");
		}
	}

	void _play() {
		audioTrack.play();

	}

	void _stop() {
		if (audioTrack != null) {
			audioTrack.stop();
			audioTrack.release();
			audioTrack = null;
		}
		// blockThread = null;
	}

	void _finish() {
	}

	void _pausePlayer() throws Exception {
		mStartPauseTime = SystemClock.elapsedRealtime();
		audioTrack.pause();
	}

	void _resumePlayer() throws Exception {
		if (mStartPauseTime >= 0)
			mPauseTime += SystemClock.elapsedRealtime() - mStartPauseTime;
		mStartPauseTime = -1;

		audioTrack.play();

	}

	void _setVolume(double volume) throws Exception {

		if (Build.VERSION.SDK_INT >= 21) {
			float v = (float) volume;
			audioTrack.setVolume(v);
		} else {
			throw new Exception("Need SDK 21");
		}

	}

    void _setVolumePan(double volume, double pan) throws Exception{
        // Ensure pan value is between -1.0 and 1.0
        pan = Math.max(-1.0f, Math.min(1.0f, (float) pan));

        float leftVolume;
        float rightVolume;

        if (pan < 0.0f) {
            // Panning to the left
            leftVolume = (float)volume * 1.0f;
            rightVolume =(float)volume * (1.0f + (float)pan);  // pan is negative, so this reduces the right volume
        } else if (pan > 0.0f) {
            // Panning to the right
            leftVolume = (float)volume * (1.0f - (float)pan);  // pan is positive, so this reduces the left volume
            rightVolume = (float)volume * 1.0f;
        } else {
            // Center
            leftVolume = (float)volume * 1.0f;
            rightVolume = (float)volume * 1.0f;
        }

        // Set the stereo volume
        audioTrack.setStereoVolume(leftVolume, rightVolume);
    }	

	void _setSpeed(double speed) throws Exception {
		float v = (float) speed;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			try {
				PlaybackParams params = audioTrack.getPlaybackParams();
				params.setSpeed(v);
				audioTrack.setPlaybackParams(params);
				return;
			} catch (Exception err) {
				mSession.logError("setSpeed: error " + err.getMessage());
			}
		}
		mSession.logError("setSpeed: not supported");
	}

	void _seekTo(long millisec) {

	}

	boolean _isPlaying() {
		return audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING;
	}

	long _getDuration() {
		return _getCurrentPosition(); // It would be better if we add what is in the input buffers and not still
						// played
	}

	long _getCurrentPosition() {
		long time;
		if (mStartPauseTime >= 0)
			time = mStartPauseTime - systemTime - mPauseTime;
		else
			time = SystemClock.elapsedRealtime() - systemTime - mPauseTime;
		return time;
	}

	// boolean busy = false;
	int feed(byte[] data) throws Exception {
		int ln = 0; // The number of bytes accepted (and perhaps played) by the device
		if (Build.VERSION.SDK_INT >= 23) {
			ln = audioTrack.write(data, 0, data.length, AudioTrack.WRITE_NON_BLOCKING);
		} else {
			ln = 0;
		}

		if (ln == 0) { // The device did not accept anything
				// We must keep trying to know when the device can accept new data
			if (blockThread != null) { // We already have a thread trying to feed the device
				System.out.println("Audio packet Lost !");
			}
			// busy = true;
			blockThread = new WriteBlockThread(data); // We start a thread to try to send the data
									// background
			blockThread.start();
		} else {
			// if (busy)
			// {
			// busy = false;
			// mSession.needSomeFood(ln);
			// if (blockThread == null)
			// mSession.onCompletion();

			// }
		}
		return ln;
	}
}

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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import android.media.AudioFocusRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Thread;

import xyz.canardoux.TauEngine.Flauto.*;
import xyz.canardoux.TauEngine.Flauto;

public class FlautoPlayer implements MediaPlayer.OnErrorListener {

	static boolean _isAndroidDecoderSupported[] = {
			true, // DEFAULT
			true, // aacADTS // OK
			Build.VERSION.SDK_INT >= 23, // opusOGG // (API 29 ???)
			false, // opusCAF /
			true, // MP3 // OK
			true, // Build.VERSION.SDK_INT >= 23, // vorbisOGG// OK
			true, // pcm16
			true, // pcm16WAV // OK
			true, // pcm16AIFF // OK
			false, // pcm16CAF // NOK
			true, // flac // OK
			true, // aacMP4 // OK
			true, // amrNB // OK
			true, // amrWB // OK
			false, // pcm8
			false, // pcmFloat32
			false, // pcmWebM
			true, // opusWebM
			true, // vorbisWebM

			/// Linear PCM32 PCM, which is a Wave file.
			true,
	};

	String extentionArray[] = {
			".aac" // DEFAULT
			, ".aac" // CODEC_AAC
			, ".opus" // opusOGG
			, "_opus.caf" // CODEC_CAF_OPUS (this is apple specific)
			, ".mp3" // CODEC_MP3
			, ".ogg" // CODEC_VORBIS
			, ".pcm" // CODEC_PCM
			, ".wav"
			, ".aiff"
			, "._pcm.caf"
			, ".flac"
			, ".mp4"
			, ".amr" // amrNB
			, ".amr" // amrWB
			, ".pcm" // pcm8
			, ".pcm" // pcmFloat323
			, ".webm" // pcmWebM
			, ".opus" // opusWebM
			, ".vorbis" // vorbisWebM

			/// Linear PCM32 PCM, which is a Wave file.
			, ".wav"
	};

	final static String TAG = "FlautoPlayer";
	long subsDurationMillis = 0;
	FlautoPlayerEngineInterface player;
	private Timer mTimer;
	final private Handler mainHandler = new Handler(Looper.getMainLooper());
	boolean pauseMode;
	FlautoPlayerCallback m_callBack;
	public t_PLAYER_STATE playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
	private double latentVolume = -1.0;
	private double latentPan = -2.0;
	private double latentSpeed = -1.0;
	private long latentSeek = -1;
	static int currentPlayerID = 0;
	private int myPlayerId = 0;

	static final String ERR_UNKNOWN = "ERR_UNKNOWN";
	static final String ERR_PLAYER_IS_NULL = "ERR_PLAYER_IS_NULL";
	static final String ERR_PLAYER_IS_PLAYING = "ERR_PLAYER_IS_PLAYING";

	/* ctor */ public FlautoPlayer(FlautoPlayerCallback callBack) {
		m_callBack = callBack;
	}

	private String getTempFileName() {
		File outputDir = Flauto.androidContext.getCacheDir(); // context being the Activity pointer
		return outputDir.getPath() + "/flutter_sound_" + myPlayerId;
	}

	public boolean openPlayer() {
		++currentPlayerID;
		myPlayerId = currentPlayerID;
		latentVolume = -1.0;
		latentSpeed = -1.0;
		latentSeek = -1;
		playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
		m_callBack.openPlayerCompleted(true);
		return true;
	}

	public void closePlayer() {
		stop();
		playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
	}

	public t_PLAYER_STATE getPlayerState() {
		if (player == null)
			return t_PLAYER_STATE.PLAYER_IS_STOPPED;
		if (player._isPlaying()) {
			if (pauseMode)
				throw new RuntimeException();
			return t_PLAYER_STATE.PLAYER_IS_PLAYING;
		}
		return pauseMode ? t_PLAYER_STATE.PLAYER_IS_PAUSED : t_PLAYER_STATE.PLAYER_IS_STOPPED;
	}

	public boolean startPlayerFromMic(int numChannels, int sampleRate, int bufferSize,
			boolean enableVoiceProcessing) {
		stop(); // To start a new clean playback

		try {
			player = new FlautoPlayerEngineFromMic(this);
			player._startPlayer(null, sampleRate, numChannels, bufferSize, enableVoiceProcessing, this);
			play();
		} catch (Exception e) {
			logError("startPlayer() exception");
			return false;
		}
		return true;
	}

	public boolean startPlayer(
			t_CODEC codec,
			String fromURI,
			byte[] dataBuffer,
			int numChannels,
			int sampleRate,
			// boolean enableVoiceProcessing, // Not used on Android
			int bufferSize) {
		stop(); // To start a new clean playback
		if (dataBuffer != null) {
			try {
				String fileName = getTempFileName();
				deleteTempFile(); // Delete old file if exists
				File f = new File(fileName);
				FileOutputStream fos = new FileOutputStream(f);
				fos.write(dataBuffer);
				fromURI = f.getPath();
			} catch (Exception e) {
				return false;
			}
		}

		try {
			if (fromURI == null && codec == t_CODEC.pcm16) {
				player = new FlautoPlayerEngine();
			} else {
				player = new FlautoPlayerMedia(this);
			}
			String path = Flauto.getPath(fromURI);

			player._startPlayer(path, sampleRate, numChannels, bufferSize, false, this);
			play();
		} catch (Exception e) {
			logError("startPlayer() exception");
			return false;
		}
		return true;
	}

	public int feed(byte[] data) throws Exception {
		if (player == null) {
			throw new Exception("feed() : player is null");
		}

		try {
			int ln = player.feed(data);
			assert (ln >= 0);
			return ln;
		} catch (Exception e) {
			logError("feed() exception");
			throw e;
		}
	}

	public void needSomeFood(int ln) {
		if (ln < 0)
			throw new RuntimeException();
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				m_callBack.needSomeFood(ln);
			}
		});
	}

	public boolean onError(MediaPlayer mp, int what, int extra) {
		// ... react appropriately ...
		// The MediaPlayer has moved to the Error state, must be reset!
		return false;
	}

	// listener called when media player has completed playing.
	public void onCompletion() {
		/*
		 * Reset player.
		 */
		logDebug("Playback completed.");
		// stop();// We don't close automaticaly when finished'
		playerState = t_PLAYER_STATE.PLAYER_IS_PAUSED;
		m_callBack.audioPlayerDidFinishPlaying(true);
	}

	// Listener called when media player has completed preparation.
	public void onPrepared() {
		logDebug("mediaPlayer prepared and started");

		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				long duration = 0;
				try {
					duration = player._getDuration();
				} catch (Exception e) {
					System.out.println(e.toString());
				}
				playerState = t_PLAYER_STATE.PLAYER_IS_PLAYING;

				m_callBack.startPlayerCompleted(true, duration);
			}
		});
		/*
		 * Set timer task to send event to RN.
		 */
	}

	public void stopPlayer() {
		stop();
		playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
		m_callBack.stopPlayerCompleted(true);
	}

	void cancelTimer() {
		if (mTimer != null)
			mTimer.cancel();
		mTimer = null;
	}


	void setTimer(long duration) {
		cancelTimer();
		subsDurationMillis = duration;
		if (player == null || duration == 0)
			return;

		if (subsDurationMillis > 0) {
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							try {
								if (player != null) {

									long position = player._getCurrentPosition();
									long duration = player._getDuration();
									if (position > duration) {
										position = duration;
									}
									m_callBack.updateProgress(position, duration);
								}
							} catch (Exception e) {
								logDebug("Exception: " + e.toString());
								stopPlayer();
							}
						}
					});

				}
			};

			mTimer = new Timer();

			mTimer.schedule(task, 0, duration);
		}

	}

	private void deleteTempFile() {
		String fileName = getTempFileName();
		try {
			File fdelete = new File(fileName);
			if (fdelete.exists()) {
				if (fdelete.delete()) {
					logDebug("file Deleted :" + fileName);
				} else {
					logError("Cannot delete file " + fileName);
				}
			}

		} catch (Exception e) {
			return;
		}

	}

	void stop() {
		deleteTempFile();
		cancelTimer();
		pauseMode = false;
		if (player != null)
			player._stop();
		player = null;
	}

	public boolean play() {
		if (player == null) {
			return false;
		}
		try {	
			if (latentVolume >= 0 && latentPan >= -1) {
				setVolumePan(latentVolume,latentPan);
			} else if (latentVolume >= 0){
				setVolume(latentVolume);
			}

			if (latentSpeed >= 0) {
				setSpeed(latentSpeed);
			}
			if (subsDurationMillis > 0)
				setTimer(subsDurationMillis);
			if (latentSeek >= 0) {
				seekToPlayer(latentSeek);
			}

		} catch (Exception e) {

		}
		player._play();
		return true;
	}

	public boolean isDecoderSupported(t_CODEC codec) {
		return _isAndroidDecoderSupported[codec.ordinal()];
	}

	public boolean pausePlayer() {
		try {
			cancelTimer();
			if (player == null) {
				m_callBack.resumePlayerCompleted(false);
				return false;
			}
			player._pausePlayer();
			pauseMode = true;
			playerState = t_PLAYER_STATE.PLAYER_IS_PAUSED;
			m_callBack.pausePlayerCompleted(true);

			return true;
		} catch (Exception e) {
			logError("pausePlay exception: " + e.getMessage());
			return false;
		}

	}

	public boolean resumePlayer() {
		try {
			if (player == null) {
				// m_callBack.resumePlayerCompleted(false);
				return false;
			}
			player._resumePlayer();
			pauseMode = false;
			playerState = t_PLAYER_STATE.PLAYER_IS_PLAYING;
			setTimer(subsDurationMillis);
			m_callBack.resumePlayerCompleted(true);

			return true;
		} catch (Exception e) {
			logError("mediaPlayer resume: " + e.getMessage());
			return false;
		}
	}

	public boolean seekToPlayer(long millis) {

		if (player == null) {
			latentSeek = millis;
			return false;
		}

		logDebug("seekTo: " + millis);
		latentSeek = -1;
		player._seekTo(millis);
		return true;
	}

	public boolean setVolume(double volume) {
		try {
			latentVolume = volume;

			if (player == null) {
				// logError( "setVolume(): player is null" );
				return false;
			}

			// float mVolume = (float) volume;
			player._setVolume(volume);
			return true;
		} catch (Exception e) {
			logError("setVolume: " + e.getMessage());
			return false;
		}
	}

	public boolean setVolumePan(double volume, double pan) {
		try {
			latentVolume = volume;
			latentPan = pan;
			if (player == null) {
				// logError( "setVolume(): player is null" );
				return false;
			}

			// float mVolume = (float) volume;
			player._setVolumePan(volume,pan);
			return true;
		} catch (Exception e) {
			logError("setVolume: " + e.getMessage());
			return false;
		}
	}

	public boolean setSpeed(double speed) {
		try {

			latentSpeed = speed;
			if (player == null) {
				return false;
			}

			player._setSpeed(speed);
			return true;
		} catch (Exception e) {
			logError("setSpeed: " + e.getMessage());
			return false;
		}
	}

	public void setSubscriptionDuration(long duration) {
		subsDurationMillis = duration;
		if (player != null)
			setTimer(subsDurationMillis);
	}

	public Map<String, Object> getProgress() {
		long position = 0;
		long duration = 0;
		if (player != null) {
			position = player._getCurrentPosition();
			duration = player._getDuration();
		}
		if (position > duration) {
			position = duration;
		}

		Map<String, Object> dic = new HashMap<String, Object>();
		dic.put("position", position);
		dic.put("duration", duration);
		dic.put("playerStatus", getPlayerState().ordinal()); // An int because necessary with Flutter Channels
		return dic;
	}

	void logDebug(String msg) {
		m_callBack.log(t_LOG_LEVEL.DBG, msg);
	}

	void logError(String msg) {
		m_callBack.log(t_LOG_LEVEL.ERROR, msg);
	}

}

package xyz.canardoux.TauEngine;
/*
 * Copyright 2018, 2019, 2020, 2021 canardoux.
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
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Build;
import android.os.SystemClock;
import android.media.MediaRecorder;

import java.lang.Thread;

import xyz.canardoux.TauEngine.Flauto.*;

//-------------------------------------------------------------------------------------------------------------



class FlautoPlayerEngineFromMic extends FlautoPlayerEngineInterface
{
	final static String             TAG                = "EngineFromMic";


	int[] tabCodec =
		{
			AudioFormat.ENCODING_DEFAULT, // DEFAULT
			AudioFormat.ENCODING_AAC_LC, // aacADTS
			0, // opusOGG
			0, // opusCAF
			AudioFormat.ENCODING_MP3, // MP3 // Not used
			0, // vorbisOGG
			AudioFormat.ENCODING_PCM_16BIT, // pcm16
			AudioFormat.ENCODING_PCM_16BIT, // pcm16WAV
			0, // pcm16AIFF
			0, // pcm16CAF
			0, // flac
			0, // aacMP4
			0, // amrNB
			0, // amrWB
		};


	AudioTrack audioTrack = null;
	int sessionId = 0;
	long mPauseTime = 0;
	long mStartPauseTime = -1;
	long systemTime = 0;
	int bufferSize = 0;
	FlautoPlayer mSession = null;

	AudioRecord recorder;
	public              int     subsDurationMillis    = 10;

	private boolean isRecording = false;
	_pollingRecordingData thePollingThread = null;



	public class _pollingRecordingData extends Thread
	{

		void _feed(byte[] data, int ln) throws Exception
		{
			int lnr = 0;
			if ( Build.VERSION.SDK_INT >= 23 )
			{
				 lnr = audioTrack.write(data, 0, ln, AudioTrack.WRITE_NON_BLOCKING);
			} else
			{
				 lnr = audioTrack.write(data, 0, ln);
			}
			if (lnr != ln)
			{
				System.out.println("feed error: some audio data are lost");
			}
		}

		public void run()
		{

			int n = 0;
			int r = 0;
			byte[] byteBuffer = new byte[bufferSize];
			while (isRecording)
			{
				try
				{
					if (Build.VERSION.SDK_INT >= 23)
					{
						n = recorder.read(byteBuffer, 0, bufferSize, AudioRecord.READ_BLOCKING);
					} else
					{
						n = recorder.read(byteBuffer, 0, bufferSize);
					}
					final int ln = n;

					if (n > 0)
					{
						r += n;

						try
						{
							_feed(byteBuffer, ln);
						} catch (Exception err)
						{
							mSession.logError("feed error" + err.getMessage());
						}
					} else
					{
						mSession.logError("feed error: ln = 0" );
						//break;
					}
				} catch (Exception e)
				{
					System.out.println(e);
					break;
				}
			}
			thePollingThread = null; // finished for me
		}

	}




	/* ctor */ FlautoPlayerEngineFromMic(FlautoPlayer theSession) throws Exception
	{
		mSession = theSession;
		if ( Build.VERSION.SDK_INT >= 21 )
		{

			AudioManager audioManager = (AudioManager) Flauto.androidContext.getSystemService(Context.AUDIO_SERVICE);
			sessionId = audioManager.generateAudioSessionId();
		} else
		{
			throw new Exception("Need SDK 21");
		}
	}

	void startPlayerSide(int sampleRate, Integer numChannels, int bufferSize, boolean voiceAudioProcessing) throws Exception
	{
		if ( Build.VERSION.SDK_INT >= 21 )
		{
			AudioAttributes attributes = new AudioAttributes.Builder()
				.setLegacyStreamType(AudioManager.STREAM_MUSIC)
				.setUsage(AudioAttributes.USAGE_MEDIA)
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
				.build();

			AudioFormat format = new AudioFormat.Builder()
				.setEncoding(AudioFormat.ENCODING_PCM_16BIT)
				.setSampleRate(sampleRate)
				.setChannelMask(numChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO)
				.build();
			audioTrack = new AudioTrack(attributes, format, bufferSize, AudioTrack.MODE_STREAM, sessionId);
			mPauseTime = 0;
			mStartPauseTime = -1;
			systemTime = SystemClock.elapsedRealtime();

			mSession.onPrepared(); // Maybe too early. Should be after _play()
		} else
		{
			throw new Exception("Need SDK 21");
		}

	}

	void _play()
	{
		audioTrack.play();
	}

	public void startRecorderSide
		(
			t_CODEC codec,
			Integer sampleRate,
			Integer numChannels,
			int _bufferSize,
			Boolean voiceAudioProcessing // Not used on Android
		) throws Exception
	{
		if ( Build.VERSION.SDK_INT < 21)
			throw new Exception ("Need at least SDK 21");
		int channelConfig = (numChannels == 1) ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
		int audioFormat = tabCodec[codec.ordinal()];
		bufferSize = AudioRecord.getMinBufferSize
			(
				sampleRate,
				channelConfig,
				tabCodec[codec.ordinal()]
			) ;// !!!!! * 2 ???

		bufferSize = Math.max(bufferSize, _bufferSize);
		recorder = new AudioRecord(
			MediaRecorder.AudioSource.MIC,
			sampleRate,
			channelConfig,
			audioFormat,
			bufferSize
		);

		if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
		{
			recorder.startRecording();
			isRecording = true;
			assert (thePollingThread == null);
			thePollingThread = new _pollingRecordingData();
			thePollingThread.start();
		} else
		{
			throw new Exception("Cannot initialize the AudioRecord");
		}

	}



	void _startPlayer
		(
			String path,
			int sampleRate,
			int numChannels,
			int bufferSize,
			boolean enableVoiceProcessing, // not used on android
			FlautoPlayer aPlayer
		) throws Exception
	{
		startPlayerSide(sampleRate, numChannels, bufferSize, enableVoiceProcessing);
		startRecorderSide(Flauto.t_CODEC.pcm16, sampleRate, numChannels, bufferSize, enableVoiceProcessing);
		mSession = aPlayer;

	}


	void _stop()
	{

		if (null != recorder)
		{
			try
			{
				recorder.stop();
			} catch ( Exception e )
			{
			}

			try
			{
				isRecording = false;
				recorder.release();
			} catch ( Exception e )
			{
			}
			recorder = null;
		}

		if (audioTrack != null)
		{
			audioTrack.stop();
			audioTrack.release();
			audioTrack = null;
		}
	}

	void _finish()
	{
	}



	void _pausePlayer() throws Exception
	{
		mStartPauseTime = SystemClock.elapsedRealtime ();
		audioTrack.pause();
	}


	void _resumePlayer() throws Exception
	{
		if (mStartPauseTime >= 0)
			mPauseTime += SystemClock.elapsedRealtime () - mStartPauseTime;
		mStartPauseTime = -1;

		audioTrack.play();

	}


	void _setVolume(double volume)  throws Exception
	{
		mSession.logError("setVolume: not implemented" );
	}

	void _setVolumePan(double volume, double pan)  throws Exception
	{
		mSession.logError("setVolumePan: not implemented" );
	}

	void _setSpeed(double speed)  throws Exception
	{
		mSession.logError("setSpeed: not implemented" );
	}


	void _seekTo(long millisec)
	{
		mSession.logError("seekTo: not implemented" );
	}


	boolean _isPlaying()
	{
		return audioTrack.getPlayState () == AudioTrack.PLAYSTATE_PLAYING;
	}


	long _getDuration()
	{
		return 0;
	}


	long _getCurrentPosition()
	{
		return 0;
	}

	int feed(byte[] data) throws Exception
	{
		mSession.logError("feed error: not implemented");
		return -1;
	}

}

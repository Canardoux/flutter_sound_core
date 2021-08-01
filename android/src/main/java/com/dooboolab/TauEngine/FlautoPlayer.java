package com.dooboolab.TauEngine;
/*
 * Copyright 2018, 2019, 2020 Dooboolab.
 *
 * This file is part of the Tau project.
 *
 * Tau is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3 (LGPL-V3), as published by
 * the Free Software Foundation.
 *
 * Tau is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the Tau project.  If not, see <https://www.gnu.org/licenses/>.
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

import com.dooboolab.TauEngine.Flauto.*;
import com.dooboolab.TauEngine.Flauto;

public class FlautoPlayer extends FlautoSession implements MediaPlayer.OnErrorListener
{

	static boolean _isAndroidDecoderSupported[] = {
		true, // DEFAULT
		true, // aacADTS				// OK
		Build.VERSION.SDK_INT >= 23, // opusOGG	// (API 29 ???)
		Build.VERSION.SDK_INT >= 23, // opusCAF				/
		true, // MP3					// OK
		true, //Build.VERSION.SDK_INT >= 23, // vorbisOGG// OK
		true, // pcm16
		true, // pcm16WAV				// OK
		true, // pcm16AIFF				// OK
		true, // pcm16CAF				// NOK
		true, // flac					// OK
		true, // aacMP4					// OK
		true, // amrNB					// OK
		true, // amrWB					// OK
		false, // pcm8
		false, // pcmFloat32
		false, // pcmWebM
		true, // opusWebM
		true, // vorbisWebM
	};


	String extentionArray[] = {
		".aac" // DEFAULT
		, ".aac" // CODEC_AAC
		, ".opus" // CODEC_OPUS
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
	};




	final static  String           TAG         = "FlautoPlayer";
	//final         PlayerAudioModel model       = new PlayerAudioModel ();
	long subsDurationMillis = 0;
	//MediaPlayer mediaPlayer                    = null;
	FlautoPlayerEngineInterface player;
	private       Timer            mTimer   ;
	final private Handler          mainHandler = new Handler (Looper.getMainLooper ());
	boolean pauseMode;
	FlautoPlayerCallback m_callBack;
	public		t_PLAYER_STATE 		playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
	private double latentVolume = -1.0;
	private double latentSpeed = -1.0;
	private long latentSeek = -1;


	static final String ERR_UNKNOWN           = "ERR_UNKNOWN";
	static final String ERR_PLAYER_IS_NULL    = "ERR_PLAYER_IS_NULL";
	static final String ERR_PLAYER_IS_PLAYING = "ERR_PLAYER_IS_PLAYING";

	/* ctor */ public FlautoPlayer(FlautoPlayerCallback callBack)
	{
		m_callBack = callBack;
	}


	public boolean openPlayer (t_AUDIO_FOCUS focus, t_SESSION_CATEGORY category, t_SESSION_MODE sessionMode, int audioFlags, t_AUDIO_DEVICE audioDevice)
	{
		latentVolume = -1.0;
		latentSpeed = -1.0;
		latentSeek = -1;
		boolean r = setAudioFocus(focus, category, sessionMode, audioFlags, audioDevice);
		playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
		m_callBack.openPlayerCompleted(r);
		return r;
	}

	public void closePlayer ( )
	{
		stop();
		if (hasFocus)
			abandonFocus();
		releaseSession();
		playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
		m_callBack.closePlayerCompleted(true);
	}

	public t_PLAYER_STATE getPlayerState()
	{
		if (player == null)
			return t_PLAYER_STATE.PLAYER_IS_STOPPED;
		if (player._isPlaying())
		{
			if (pauseMode)
				throw new RuntimeException();
			return t_PLAYER_STATE.PLAYER_IS_PLAYING;
		}
		return pauseMode ? t_PLAYER_STATE.PLAYER_IS_PAUSED : t_PLAYER_STATE.PLAYER_IS_STOPPED;
	}

	public boolean startPlayerFromMic (int numChannels, int sampleRate, int blockSize )
	{
		//if ( ! hasFocus ) // We always require focus because it could have been abandoned by another Session
		{
			requestFocus ();
		}
		stop(); // To start a new clean playback

		try
		{
			player = new FlautoPlayerEngineFromMic(this);
			player._startPlayer(null,  sampleRate, numChannels, blockSize, this);
			play();
		}
		catch ( Exception e )
		{
			logError ("startPlayer() exception" );
			return false;
		}
		return true;
	}

	public boolean startPlayer (t_CODEC codec, String fromURI, byte[] dataBuffer, int numChannels, int sampleRate, int blockSize )
	{

		//if ( ! hasFocus ) // We always require focus because it could have been abandoned by another Session
		{
			requestFocus ();
		}

		if (dataBuffer != null)
		{
			try
			{
				File             f   = File.createTempFile ( "flauto_buffer-" + Integer.toString(slotNo), extentionArray[ codec.ordinal () ] );
				FileOutputStream fos = new FileOutputStream ( f );
				fos.write ( dataBuffer );
				fromURI = f.getPath();
			}
			catch ( Exception e )
			{
				return false;
			}
		}

		stop(); // To start a new clean playback

		try
		{
			if (fromURI == null && codec == t_CODEC.pcm16)
			{
				player = new FlautoPlayerEngine();
			} else
			{
				player = new FlautoPlayerMedia(this);
			}
			String path = Flauto.getPath(fromURI);

			player._startPlayer(path,  sampleRate, numChannels, blockSize, this);
			play();
		}
		catch ( Exception e )
		{
			logError (  "startPlayer() exception" );
			return false;
		}
		return true;
	}

	public int feed( byte[] data) throws  Exception
	{
		if (player == null)
		{
			throw new Exception("feed() : player is null");
		}

		try
		{
			int ln = player.feed(data);
			assert (ln >= 0);
			return ln;
		} catch (Exception e)
		{
			logError (  "feed() exception" );
			throw e;
		}
	}

	public void needSomeFood(int ln)
	{
		if (ln < 0)
			throw new RuntimeException();
		mainHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				m_callBack.needSomeFood(ln);
			}
		});
	}

	public boolean startPlayerFromTrack
	(
		FlautoTrack track,
		boolean canPause,
		boolean canSkipForward,
		boolean canSkipBackward,
		int progress,
		int duration,
		boolean removeUIWhenStopped,
		boolean defaultPauseResume
	)
	{
		logError ( "Must be initialized With UI" );
		return false;
	}

	public boolean onError(MediaPlayer mp, int what, int extra)
	{
		// ... react appropriately ...
		// The MediaPlayer has moved to the Error state, must be reset!
		return false;
	}


	// listener called when media player has completed playing.
	public void onCompletion()
	{
			/*
			 * Reset player.
			 */
			logDebug("Playback completed.");
			stop();
			playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
			m_callBack.audioPlayerDidFinishPlaying(true);
	}

	// Listener called when media player has completed preparation.
	public void onPrepared( )
	{
		logDebug ("mediaPlayer prepared and started");

		mainHandler.post(new Runnable()
		{
			@Override
			public void run() {
				long duration = 0;
				try
				{
					 duration = player._getDuration();
				} catch(Exception e)
				{
					System.out.println(e.toString());
				}
				//invokeMethodWithInteger("startPlayerCompleted", (int) duration);
				//Map<String, Object> dico = new HashMap<String, Object> ();
				//dico.put( "duration", (int) duration);
				//dico.put( "state",  (int)getPlayerState());
				playerState = t_PLAYER_STATE.PLAYER_IS_PLAYING;

				m_callBack.startPlayerCompleted(true, duration);
			}
		});
		/*
		 * Set timer task to send event to RN.
		 */
	}


	public void stopPlayer ( )
	{
		stop();
		playerState = t_PLAYER_STATE.PLAYER_IS_STOPPED;
		m_callBack.stopPlayerCompleted(true);
	}

	void cancelTimer()
	{
		if (mTimer != null)
			mTimer.cancel ();
		mTimer = null;
	}
	void setTimer(long duration)
	{
		cancelTimer();
		subsDurationMillis = duration;
		if (player == null || duration == 0)
			return;

		if (subsDurationMillis > 0)
		{
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					mainHandler.post(new Runnable()
					{
						@Override
						public void run()
						{
							try
							{
								if (player != null)
								{

									long position = player._getCurrentPosition();
									long duration = player._getDuration();
									if (position > duration)
									{
										position = duration;
									}
	/*
										Map<String, Object> dic = new HashMap<String, Object>();
										dic.put("position", position);
										dic.put("duration", duration);
										dic.put("playerStatus", getPlayerState());
	*/
									m_callBack.updateProgress(position, duration);
								}
							} catch (Exception e)
							{
								logDebug( "Exception: " + e.toString());
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

	void stop()
	{
		cancelTimer();
		pauseMode = false;
		if (player != null)
			player._stop();
		player = null;

	}

	public boolean play()
	{
			if (player == null)
			{
					return false;
			}
			try
			{
				if (latentVolume >= 0)
				{
					setVolume(latentVolume);
				}
				if (latentSpeed >= 0)
				{
					setSpeed(latentSpeed);
				}
				if (subsDurationMillis > 0)
					setTimer(subsDurationMillis);
				if (latentSeek >= 0)
				{
					seekToPlayer(latentSeek);
				}


			}catch (Exception e)
			{

			}
			player._play();
			return true;
	}

	public boolean isDecoderSupported (t_CODEC codec )
	{
		return _isAndroidDecoderSupported[ codec.ordinal() ];
	}

	public boolean pausePlayer (  )
	{
		try
		{
			cancelTimer();
			if (player == null)
			{
				m_callBack.resumePlayerCompleted(false);
				return false;
			}
			player._pausePlayer();
			pauseMode = true;
			playerState = t_PLAYER_STATE.PLAYER_IS_PAUSED;
			m_callBack.pausePlayerCompleted(true);

			return true;
		}
		catch ( Exception e )
		{
			logError( "pausePlay exception: " + e.getMessage () );
			return false;
		}

	}

	public boolean resumePlayer (  )
	{
		try
		{
			if (player == null)
			{
				//m_callBack.resumePlayerCompleted(false);
				return false;
			}
			player._resumePlayer();
			pauseMode = false;
			playerState = t_PLAYER_STATE.PLAYER_IS_PLAYING;
			setTimer(subsDurationMillis);
			m_callBack.resumePlayerCompleted(true);

			return true;
		}
		catch ( Exception e )
		{
			logError( "mediaPlayer resume: " + e.getMessage () );
			return false;
		}
	}

	public boolean seekToPlayer (long millis)
	{

		if ( player == null )
		{
			latentSeek = millis;
			return false;
		}


		logDebug("seekTo: " + millis );
		latentSeek = -1;
		player._seekTo ( millis );
		return true;
	}

	public boolean setVolume ( double volume )
	{
		try
		{

			latentVolume = volume;
			if (player == null) {
				//logError( "setVolume(): player is null" );
				return false;
			}

			//float mVolume = (float) volume;
			player._setVolume(volume);
			return true;
		} catch(Exception e)
		{
			logError ("setVolume: " + e.getMessage () );
			return false;
		}
	}


	public boolean setSpeed ( double speed )
	{
		try
		{

			latentSpeed = speed;
			if (player == null) {
				return false;
			}

			player._setSpeed(speed);
			return true;
		} catch(Exception e)
		{
			logError ("setSpeed: " + e.getMessage () );
			return false;
		}
	}


	public void setSubscriptionDuration (long duration)
	{
		subsDurationMillis = duration;
		if (player != null)
			setTimer(subsDurationMillis);
	}

	public boolean androidAudioFocusRequest ( int focusGain )
	{

		if ( Build.VERSION.SDK_INT >= 26 )
		{
			audioFocusRequest = new AudioFocusRequest.Builder ( focusGain )
				// .setAudioAttributes(mPlaybackAttributes)
				// .setAcceptsDelayedFocusGain(true)
				// .setWillPauseWhenDucked(true)
				// .setOnAudioFocusChangeListener(this, mMyHandler)
				.build ();
			return true;
		} else
		{
			return false;
		}
	}


	public boolean setActive (Boolean enabled )
	{

		Boolean b = false;
		try
		{
			if ( enabled )
			{
				b = requestFocus ();
			} else
			{

				b = abandonFocus ();
			}
		}
		catch ( Exception e )
		{
			b = false;
		}
		return b;
	}

	public Map<String, Object> getProgress (  )
	{
		long position = 0;
		long duration = 0;
		if ( player != null ) {
			 position = player._getCurrentPosition();
			 duration = player._getDuration();
		}
		if (position > duration)
		{
			position = duration;
		}

		Map<String, Object> dic = new HashMap<String, Object> ();
		dic.put ( "position", position );
		dic.put ( "duration", duration );
		dic.put ( "playerStatus", getPlayerState() );
		return dic;
	}

	public void nowPlaying
	(
		FlautoTrack track,
		boolean canPause,
		boolean canSkipForward,
		boolean canSkipbackward,
		boolean defaultPauseResume,
		int progress,
		int duration
	)
	{
		throw new RuntimeException(); // TODO
	}


	public void setUIProgressBar (int progress, int duration)
	{
		throw new RuntimeException(); // TODO
	}


	void logDebug (String msg)
	{
		m_callBack.log ( t_LOG_LEVEL.DBG , msg);
	}


	void logError (String msg)
	{
		m_callBack.log ( t_LOG_LEVEL.ERROR , msg);
	}

}


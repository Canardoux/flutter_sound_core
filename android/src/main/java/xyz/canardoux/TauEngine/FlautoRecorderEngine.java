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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.lang.Math;
import xyz.canardoux.TauEngine.Flauto.*;
import xyz.canardoux.TauEngine.FlautoRecorderInterface;
import xyz.canardoux.TauEngine.FlautoRecorder;


public class FlautoRecorderEngine
	implements FlautoRecorderInterface
{
		private AudioRecord recorder = null;
		//private Thread recordingThread = null;
		private boolean isRecording = false;
		private double maxAmplitude = 0;
		private double previousAmplitude = 0;
		private int nbrSamples;

	String filePath;
		int totalBytes = 0;
		t_CODEC codec;
		Runnable p;
		FlautoRecorder session = null;
		FileOutputStream outputStream = null;
		final private Handler mainHandler = new Handler (Looper.getMainLooper ());




	private short getShort(byte argB1, byte argB2) {
		return (short)(argB1 | (argB2 << 8));
	}

	private void writeAudioDataToFile(t_CODEC codec, int sampleRate, int numChannels, String aFilePath) throws IOException
	{
			// Write the output audio in byte
			System.out.println("---> writeAudioDataToFile");
			totalBytes = 0;
			outputStream = null;
			filePath = aFilePath;
			if (filePath != null)
			{
				outputStream = new FileOutputStream(filePath);

				if (codec == t_CODEC.pcm16WAV) {
					FlautoWaveHeader header = new FlautoWaveHeader
						(
							FlautoWaveHeader.FORMAT_PCM,
							(short) numChannels,
							sampleRate,
							(short) 16,
							100000 // total number of bytes // Will be orverwritten when stop()

						);
					header.write(outputStream);
				}
			}
			System.out.println("<--- writeAudioDataToFile");
	}

	void closeAudioDataFile(String filepath) throws Exception
	{

		if (outputStream != null) {
			outputStream.close();
			if (codec == t_CODEC.pcm16WAV) {
				RandomAccessFile fh = new RandomAccessFile(filePath, "rw");
				fh.seek(4);
				int x = totalBytes + 36;
				fh.write(x >> 0);
				fh.write(x >> 8);
				fh.write(x >> 16);
				fh.write(x >> 24);


				fh.seek(FlautoWaveHeader.HEADER_LENGTH - 4);
				fh.write(totalBytes >> 0);
				fh.write(totalBytes >> 8);
				fh.write(totalBytes >> 16);
				fh.write(totalBytes >> 24);
				fh.close();
			}
		}

	}

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

			/// Raw PCM Linear 8
			0,

			/// Raw PCM with 32 bits Floating Points
			AudioFormat.ENCODING_PCM_FLOAT,
			
			/// pcm with a WebM format
			0,
			
			/// Opus with a WebM format
			0,
			
			/// Vorbis with a WebM format
			0,
		
			/// Linear PCM32 PCM, which is a Wave file.
			AudioFormat.ENCODING_PCM_FLOAT,
		};



	int writeData32Interleavedxxx(
			t_CODEC theCodec,
			Integer numChannels,
			Boolean interleaved,
			int bufferSize)
	{
		int n = 0;
		int r = 0;
		ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);
		FloatBuffer floatBuffer = FloatBuffer.allocate(bufferSize/4);

		while (isRecording ) {
			//ShortBuffer shortBuffer = ShortBuffer.allocate(bufferSize/2);
			try {
				// gets the voice output from microphone to byte format
				if ( Build.VERSION.SDK_INT >= 23 )
				{
					n = recorder.read(floatBuffer.array(), 0, bufferSize/4, AudioRecord.READ_NON_BLOCKING);

					if (codec == t_CODEC.pcmFloat32 ) {
						if (interleaved) {
							byteBuffer.asFloatBuffer().put(floatBuffer);
						}
					} else {

						n = recorder.read(byteBuffer.array(), 0, bufferSize, AudioRecord.READ_NON_BLOCKING);
					}

				}
				else
				{
					throw new Exception("Android SDK must be >= 23");
				}
				final int elementCount = n;//2 * n;

				if (n > 0) {
					totalBytes += n;
					r += n;
					if (outputStream != null) {
						outputStream.write(byteBuffer.array(), 0, elementCount);
					} else {
						if (codec == t_CODEC.pcmFloat32 )
						{
							if (!interleaved) {
								ArrayList<float[]> data = new ArrayList();
								for (int channel = 0; channel < numChannels; ++channel)
								{
									FloatBuffer buffer = FloatBuffer.allocate(n/numChannels);
									for (int i = 0; i < n/numChannels; ++i)
									{
										buffer.put( floatBuffer.get(numChannels * i + channel));
									}
									data.add(buffer.array());
								}
							}
							ByteBuffer buf = ByteBuffer.allocate(bufferSize);
							floatBuffer.rewind();
							buf.rewind();
							buf.asFloatBuffer().put(floatBuffer);
							mainHandler.post(new Runnable() {
								@Override
								public void run() {
									session.recordingData(Arrays.copyOfRange(buf.array(), 0, elementCount));
								}
							});
						} else
						{
							mainHandler.post(new Runnable() {
								@Override
								public void run() {
									session.recordingData(Arrays.copyOfRange(byteBuffer.array(), 0, elementCount));
								}
							});

						}

					}

					if (codec == t_CODEC.pcmFloat32 && interleaved)
					{
						float m = 0;
						for (int i = 0; i < n / 4; ++i)
						{
							float curSample = floatBuffer.get(i);
							if (curSample > m)
							{
								m = curSample;
							}
						}
						m *= 0x7FFF;
						if ( m > maxAmplitude )
						{
							maxAmplitude = m;
						}

						++ nbrSamples;

					} else
					if (codec == t_CODEC.pcm16 || codec == t_CODEC.pcm16WAV)
					{
						for (int i = 0; i < n / 2; ++i)
						{
							short curSample = getShort(byteBuffer.array()[i * 2], byteBuffer.array()[i * 2 + 1]);
							if (curSample > maxAmplitude)
							{
								maxAmplitude = curSample;
							}
						}
						++ nbrSamples;

					}


				} else
				{
					break;
				}
				if ( Build.VERSION.SDK_INT < 23 ) // We must break the loop, because n is always 1024 (READ_BLOCKING_MODE)
					break;
			} catch (Exception e) {
				System.out.println(e);
				break;
			}
		}
		if (isRecording)
			mainHandler.post(p);

		return r;

	}


	void computeMaxAmplitude16(ByteBuffer byteBuffer)
	{
		byte[] array = byteBuffer.array();
		int n = array.length;

		for (int i = 0; i < n / 2; ++i)
		{
			short curSample = getShort(array[i * 2], array[i * 2 + 1]);
			int m = Math.abs(curSample);
			if (m > maxAmplitude)
			{
				maxAmplitude = m;
			}
		}
		++ nbrSamples;
	}

	void computeMaxAmplitude32(float[] b)
	{
		int ln = b.length;
		float m = 0;
		for (int i = 0; i < ln / 2; ++i)
		{
			float sample = Math.abs(b[i]);
			if (sample > m)
				m = sample;
		}
		m *= 0x7FFF;
		if ( m > maxAmplitude )
		{
			maxAmplitude = m;
		}

		++ nbrSamples;
	}

	ArrayList<byte[]> uninterleaved16(Integer numChannels, byte[] b)
	{
		ArrayList<byte[]> r = new ArrayList();
		int ln = b.length;
		for (int channel = 0; channel < numChannels; ++channel)
		{
			int xln = ln/numChannels;
			byte[] x = new byte[xln];
			for (int i = 0; i < xln/2; ++i)
			{
				int pos = 2 * (channel + i * numChannels );
				if (pos >= ln)
				{
					pos = pos;
				}
				x[2*i] = b[pos]; // Little endian
				x[2*i+1]= b[pos+1];
			}
			r.add(x);
		}
		return r;
	}


	int writeData32(
			t_CODEC theCodec,
			Integer numChannels,
			Boolean interleaved,
			int bufferSize) throws Exception {
		FloatBuffer floatBuffer = FloatBuffer.allocate(bufferSize / 4);

		int n = recorder.read(floatBuffer.array(), 0, bufferSize / 4, AudioRecord.READ_NON_BLOCKING);

		if (n > 0) {
			totalBytes += n;
			ArrayList<float[]> r = new ArrayList<float[]>();

				for (int channel = 0; channel < numChannels; ++channel) {
					int frameSize = n /  numChannels;
					FloatBuffer fb = FloatBuffer.allocate(frameSize);
					for (int i = 0; i < frameSize; ++i) {
						int pos = channel + i * numChannels;
						fb.array()[i ] = floatBuffer.array()[pos];
					}
					r.add(fb.array());
				}

			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					session.recordingDataFloat32(r);
				}
			});
			computeMaxAmplitude32(floatBuffer.array());


		}
		return n;
	}


		int writeData32Interleaved(
			t_CODEC theCodec,
			Integer numChannels,
			Boolean interleaved,
			int bufferSize) throws Exception

	{
			FloatBuffer floatBuffer = FloatBuffer.allocate(bufferSize/4);

			int n = recorder.read(floatBuffer.array(), 0, bufferSize / 4, AudioRecord.READ_NON_BLOCKING);
			n *= 4;

			if (n > 0)
			{
				totalBytes += n;

				//byte[] bytearray = buf.array();
				//float toto = buf.getFloat(0);
				//float toto1 = buf.getFloat(1);
				//buf.rewind();

				if (!interleaved)
				{
					FloatBuffer fb = FloatBuffer.allocate(bufferSize/4);
					int lnx = n / numChannels;
					for (int channel = 0; channel < numChannels; ++channel)
					{
						int frameSize =  n/(4 * numChannels);
							for (int i = 0; i < frameSize; ++i)
							{
								int pos = channel + i * numChannels;
								fb.array()[i + channel * frameSize] = floatBuffer.array()[pos ];
							}
					}
					floatBuffer = fb;

				}

				ByteBuffer buf = ByteBuffer.allocate(bufferSize);
				buf.rewind();

				buf.order(ByteOrder.nativeOrder());
				floatBuffer.rewind();
				buf.rewind();
				buf.asFloatBuffer().put(floatBuffer);


					final int ln = n;
					final byte[] b = Arrays.copyOfRange(buf.array(), 0, ln);
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							session.recordingData(b);
						}
					});
					computeMaxAmplitude32(floatBuffer.array());

			}

			return n;
	}

	int writeData16(
				  t_CODEC theCodec,
				  Integer numChannels,
				  Boolean interleaved,
				  int bufferSize)
	{
				int n = 0;
				ByteBuffer byteBuffer = ByteBuffer.allocate(bufferSize);


				n = recorder.read(byteBuffer.array(), 0, bufferSize, AudioRecord.READ_NON_BLOCKING);
				if (n == 0)
					return 0;
				final int elementCount = n;


				if (interleaved) // pcmInt16 interleaved
				{
					final byte[] b = Arrays.copyOfRange(byteBuffer.array(), 0, elementCount);
					mainHandler.post(new Runnable() {
						@Override
						public void run() {
							session.recordingData(b);
						}
					});
				} else // pcmInt16 !interleaved
				{
					final byte[] b = Arrays.copyOfRange(byteBuffer.array(), 0, elementCount);
					final ArrayList<byte[]> x = uninterleaved16(numChannels, b);
					mainHandler.post(new Runnable() {
						@Override
						public void run()
						{

							session.recordingDataInt16(x);
						}
					});

				}
				computeMaxAmplitude16(byteBuffer);

				return n;

	}

	int writeData(
			t_CODEC codec,
			Integer numChannels,
			Boolean interleaved,
			int bufferSize)
	{
		int n = 0;
		while (isRecording ) {
			try {

				if (codec == t_CODEC.pcm16  || codec == t_CODEC.pcm16WAV ) {
					 n = writeData16(codec, numChannels, interleaved
							, bufferSize);
				} else
				if (interleaved) {
					 n = writeData32Interleaved(codec, numChannels, interleaved
							, bufferSize);
				} else
				{
					n = writeData32(codec, numChannels, interleaved
							, bufferSize);
				}

				if (isRecording)
					mainHandler.post(p);
				if (n == 0)
					break;
			} catch (Exception e) {
				System.out.println(e);
				break;
			}
		}
		//if (isRecording)
			//mainHandler.post(p);
		return 1;
	}


		public void _startRecorder
		(
			Integer numChannels,

			Boolean interleaved,
			Integer sampleRate,
			Integer bitRate,
			Integer bufferSize,
			t_CODEC theCodec,
			String path,
			int audioSource,
			boolean							noiseSuppression	,
			boolean							echoCancellation	,

			FlautoRecorder theSession

		) throws Exception
	{
		if ( Build.VERSION.SDK_INT < 21)
			throw new Exception ("Need at least SDK 21");
		session = theSession;
		codec = theCodec;
		int channelConfig = (numChannels == 1) ? AudioFormat.CHANNEL_IN_MONO : AudioFormat.CHANNEL_IN_STEREO;
		int audioFormat = tabCodec[codec.ordinal()];
		int minBufferSize = AudioRecord.getMinBufferSize
			(
				sampleRate,
			      	channelConfig,
				tabCodec[codec.ordinal()]
			) * 2;

			int bufLn = Math.max(minBufferSize, bufferSize);

		recorder = new AudioRecord( 	audioSource,
										sampleRate,
										channelConfig,
		                            	audioFormat,
		                            	bufLn
					);

		if (recorder.getState() == AudioRecord.STATE_INITIALIZED)
		{
			if (noiseSuppression) {
				NoiseSuppressor.create (recorder.getAudioSessionId ());
			}
			if (echoCancellation) {
				AcousticEchoCanceler.create (recorder.getAudioSessionId ());
			}
			recorder.startRecording();
			isRecording = true;
			try {
				writeAudioDataToFile(codec, sampleRate, numChannels, path);
			} catch (Exception e) {
				e.printStackTrace();
			}
			p = new Runnable() {
				@Override
				public void run() {

					if (isRecording) {
						int n = writeData( codec, numChannels, interleaved, bufLn);

					}
				}
			};
			mainHandler.post(p);
		} else
		{
			throw new Exception("Cannot initialize the AudioRecord");
		}

	}

	public void _stopRecorder (  ) throws Exception
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
		closeAudioDataFile(filePath);
	}

	public boolean pauseRecorder( )
	{
		try
		{
			recorder.stop();
			return true;
		} catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public boolean resumeRecorder(  )
	{
		try
		{
			recorder.startRecording();
			return true;
		} catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public double getMaxAmplitude ()
	{
		if (nbrSamples > 0) {
			previousAmplitude = maxAmplitude;
			maxAmplitude = 0;
			nbrSamples = 0;
		}
		return previousAmplitude;
	}
}

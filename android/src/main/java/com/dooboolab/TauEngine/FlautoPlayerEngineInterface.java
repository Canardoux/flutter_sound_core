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


abstract class FlautoPlayerEngineInterface
{
	abstract void _startPlayer(String path, int sampleRate, int numChannels, int blockSize, FlautoPlayer theSession) throws Exception;
	abstract void _stop();
	abstract void _pausePlayer() throws Exception;
	abstract void _resumePlayer() throws Exception;
	abstract void _setVolume(double volume) throws Exception;
	abstract void _setSpeed(double speed) throws Exception;
	abstract void _seekTo(long millisec);
	abstract boolean _isPlaying();
	abstract long _getDuration();
	abstract long _getCurrentPosition();
	abstract int feed(byte[] data) throws Exception;
	abstract void _finish() ;
	abstract void _play();


}

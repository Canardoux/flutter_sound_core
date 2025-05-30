package xyz.canardoux.TauEngine;
/*
 * Copyright 2018, 2019, 2020, 2021 DoobCanardouxolab.
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

import java.io.IOException;
import xyz.canardoux.TauEngine.Flauto.t_CODEC;


public interface FlautoRecorderInterface
{
	public void _startRecorder
		(
			Integer numChannels,
			Boolean interleaved,
			Integer sampleRate,
			Integer bitRate,
			Integer bufferSize,
			t_CODEC codec,
			String path,
			int audioSource,
			boolean							noiseSuppression,
			boolean							echoCancellation,

			FlautoRecorder session
		)
		throws
		IOException, Exception;
	public void _stopRecorder (  ) throws Exception;
	public boolean pauseRecorder( );
	public boolean resumeRecorder(  );
	public double getMaxAmplitude ();

}

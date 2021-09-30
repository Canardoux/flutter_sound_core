package com.dooboolab.TauEngine;
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

import java.io.IOException;
import com.dooboolab.TauEngine.Flauto.t_CODEC;


public interface FlautoRecorderInterface
{
	public void _startRecorder
		(
			Integer numChannels,
			Integer sampleRate,
			Integer bitRate,
			t_CODEC codec,
			String path,
			int audioSource,
			FlautoRecorder session
		)
		throws
		IOException, Exception;
	public void _stopRecorder (  ) throws Exception;
	public boolean pauseRecorder( );
	public boolean resumeRecorder(  );
	public double getMaxAmplitude ();

}

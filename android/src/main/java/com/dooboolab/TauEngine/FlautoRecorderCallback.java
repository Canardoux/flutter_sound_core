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

import com.dooboolab.TauEngine.Flauto.*;

public interface FlautoRecorderCallback
{
        public abstract void openRecorderCompleted(boolean success);
        public abstract void closeRecorderCompleted(boolean success);
        public abstract void startRecorderCompleted(boolean success);
        public abstract void stopRecorderCompleted(boolean success, String url);
        public abstract void pauseRecorderCompleted(boolean success);
        public abstract void resumeRecorderCompleted(boolean success);
        public abstract void updateRecorderProgressDbPeakLevel(double normalizedPeakLevel, long duration);
        public abstract void recordingData ( byte[] data);
        abstract public void log(t_LOG_LEVEL level, String msg);

}
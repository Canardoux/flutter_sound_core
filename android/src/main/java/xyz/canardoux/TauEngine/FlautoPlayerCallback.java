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

import xyz.canardoux.TauEngine.Flauto.*;

public interface FlautoPlayerCallback
{
  	abstract public void openPlayerCompleted(boolean success);
	abstract public void closePlayerCompleted(boolean success);
	abstract public void stopPlayerCompleted(boolean success);
	abstract public void pausePlayerCompleted(boolean success);
	abstract public void resumePlayerCompleted(boolean success);
	abstract public void startPlayerCompleted (boolean success, long duration);
	abstract public void needSomeFood (int ln);
	abstract public void updateProgress(long position, long duration);
	abstract public void audioPlayerDidFinishPlaying (boolean flag);
	abstract public void updatePlaybackState(t_PLAYER_STATE newState);
	abstract public void log(t_LOG_LEVEL level, String msg);
}
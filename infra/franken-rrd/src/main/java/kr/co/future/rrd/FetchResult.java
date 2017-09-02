/*
 * Copyright 2010 NCHOVY
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kr.co.future.rrd;

import java.lang.ref.WeakReference;
import java.util.List;

import kr.co.future.rrd.impl.Archive;
import kr.co.future.rrd.impl.RrdRaw;

public class FetchResult {
	private List<FetchRow> rows;
	private WeakReference<RrdRaw> rrd;

	public FetchResult(RrdRaw rrd, Archive archive, long start, long end) {
		this.rrd = new WeakReference<RrdRaw>(rrd);
		long step = rrd.getStep();
		long normalizedStartTime = start / step * step;
		long normalizedEndTime = end / step * step;
		this.rows = archive.fetchRows(normalizedStartTime, normalizedEndTime);
	}

	public List<FetchRow> getRows() {
		return rows;
	}

	public RrdRaw getRrdRaw() {
		return rrd.get();
	}
}

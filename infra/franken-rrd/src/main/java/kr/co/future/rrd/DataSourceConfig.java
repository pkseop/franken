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

public class DataSourceConfig {
	private String name;
	private DataSourceType type;
	private long minimalHeartbeat;
	private double min;
	private double max;

	public DataSourceConfig(String name, DataSourceType type, long minimalHeartbeat, double min, double max) {
		this.name = name;
		this.type = type;
		this.minimalHeartbeat = minimalHeartbeat;
		this.min = min;
		this.max = max;
	}

	public String getName() {
		return name;
	}

	public void setType(DataSourceType type) {
		this.type = type;
	}

	public DataSourceType getType() {
		return type;
	}

	public long getMinimalHeartbeat() {
		return minimalHeartbeat;
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}
}
/*
 * Copyright (C) 2011 University Wuerzburg, Computer Science VI
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package tests;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import de.d3web.we.testcase.kdom.TimeStampType;

/**
 * 
 * @author Reinhard Hatko
 * @created 17.06.2011
 */
public class TimeStampTest {

	/**
	 * Test method for
	 * {@link de.d3web.we.testcase.kdom.TimeStampType#isValid(java.lang.String)}.
	 */
	@Test
	public final void testIsValid() {

		Assert.assertTrue(TimeStampType.isValid("1ms"));
		Assert.assertTrue(TimeStampType.isValid("1s"));
		Assert.assertTrue(TimeStampType.isValid("1min"));
		Assert.assertTrue(TimeStampType.isValid("1h"));
		Assert.assertTrue(TimeStampType.isValid("1d"));

		Assert.assertTrue(TimeStampType.isValid("0ms"));
		Assert.assertTrue(TimeStampType.isValid("0s"));
		Assert.assertTrue(TimeStampType.isValid("0min"));
		Assert.assertTrue(TimeStampType.isValid("0h"));
		Assert.assertTrue(TimeStampType.isValid("0d"));

		Assert.assertTrue(TimeStampType.isValid("1ms1s1m1h1d"));
		Assert.assertTrue(TimeStampType.isValid("10ms10s10m10h10d"));

		Assert.assertFalse(TimeStampType.isValid("0.1ms"));
		Assert.assertFalse(TimeStampType.isValid("1a"));
		Assert.assertFalse(TimeStampType.isValid("0.1ms"));
		Assert.assertFalse(TimeStampType.isValid("a"));
		Assert.assertFalse(TimeStampType.isValid("1"));
		Assert.assertFalse(TimeStampType.isValid("abcd"));

	}

	/**
	 * Test method for
	 * {@link de.d3web.we.testcase.kdom.TimeStampType#getTimeInMillis(java.lang.String)}
	 * .
	 */
	@Test
	public final void testGetTimeInMillisString() {

		Assert.assertEquals(1, TimeStampType.getTimeInMillis("1ms"));
		Assert.assertEquals(1000, TimeStampType.getTimeInMillis("1s"));
		Assert.assertEquals(1000 * 60, TimeStampType.getTimeInMillis("1min"));
		Assert.assertEquals(1000 * 60 * 60, TimeStampType.getTimeInMillis("1h"));
		Assert.assertEquals(1000 * 60 * 60 * 24, TimeStampType.getTimeInMillis("1d"));

		Assert.assertEquals(1 + 1000 + 1000 * 60
				+ 1000 * 60 * 60 + 1000 * 60 * 60 * 24,
				TimeStampType.getTimeInMillis("1ms1s1min1h1d"));

		Assert.assertEquals((1 + 1000 + 1000 * 60 + 1000 * 60 * 60 + 1000 * 60 * 60 * 24) * 10,
				TimeStampType.getTimeInMillis("10ms10s10min10h10d"));

	}

	@Test
	public void millisToTimeStamp() throws Exception {
		Assert.assertEquals("1ms", TimeStampType.createTimeAsTimeStamp(1));
		Assert.assertEquals("1s", TimeStampType.createTimeAsTimeStamp(1000));
		Assert.assertEquals("1min", TimeStampType.createTimeAsTimeStamp(1000 * 60));
		Assert.assertEquals("1h", TimeStampType.createTimeAsTimeStamp(1000 * 60 * 60));
		Assert.assertEquals("1d", TimeStampType.createTimeAsTimeStamp(1000 * 60 * 60 * 24));

		Assert.assertEquals("1d1h1min1s1ms",
				TimeStampType.createTimeAsTimeStamp(1 + 1000 + 1000 * 60
				+ 1000 * 60 * 60 + 1000 * 60 * 60 * 24));

		Assert.assertEquals((1 + 1000 + 1000 * 60 + 1000 * 60 * 60 + 1000 * 60 * 60 * 24) * 10,
				TimeStampType.getTimeInMillis("10d10h10min10s10ms"));

		Random random = new Random();
		for (int i = 0; i < 10; i++) {
			long l = Math.abs(random.nextInt(2000000000));
			String timeStamp = TimeStampType.createTimeAsTimeStamp(l);
			long millis = TimeStampType.getTimeInMillis(timeStamp);
			// System.out.println(timeStamp);
			Assert.assertEquals(l, millis);

		}
	}

}

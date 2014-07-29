package ca.uhn.fhir.model.primitive;

/*
 * #%L
 * HAPI FHIR - Core Library
 * %%
 * Copyright (C) 2014 University Health Network
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.api.annotation.DatatypeDef;
import ca.uhn.fhir.model.api.annotation.SimpleSetter;
import ca.uhn.fhir.parser.DataFormatException;

@DatatypeDef(name = "instant")
public class InstantDt extends BaseDateTimeDt {

	/**
	 * The default precision for this type
	 */
	public static final TemporalPrecisionEnum DEFAULT_PRECISION = TemporalPrecisionEnum.MILLI;

	/**
	 * Constructor which creates an InstantDt with <b>no timne value</b>. Note
	 * that unlike the default constructor for the Java {@link Date} or
	 * {@link Calendar} objects, this constructor does not initialize the object
	 * with the current time.
	 * 
	 * @see #withCurrentTime() to create a new object that has been initialized
	 *      with the current time.
	 */
	public InstantDt() {
		super();
	}

	/**
	 * Create a new DateTimeDt
	 */
	public InstantDt(Calendar theCalendar) {
		setValue(theCalendar.getTime());
		setPrecision(DEFAULT_PRECISION);
		setTimeZone(theCalendar.getTimeZone());
	}

	/**
	 * Create a new DateTimeDt using an existing value. <b>Use this constructor with caution</b>,
	 * as it may create more precision than warranted (since for example it is possible to pass in
	 * a DateTime with only a year, and this constructor will convert to an InstantDt with 
	 * milliseconds precision).  
	 */
	public InstantDt(BaseDateTimeDt theDateTime) {
		setValue(theDateTime.getValue());
		setPrecision(DEFAULT_PRECISION);
		setTimeZone(theDateTime.getTimeZone());
	}

	/**
	 * Create a new DateTimeDt
	 */
	@SimpleSetter(suffix = "WithMillisPrecision")
	public InstantDt(@SimpleSetter.Parameter(name = "theDate") Date theDate) {
		setValue(theDate);
		setPrecision(DEFAULT_PRECISION);
		setTimeZone(TimeZone.getDefault());
	}

	/**
	 * Constructor which accepts a date value and a precision value. Valid
	 * precisions values for this type are:
	 * <ul>
	 * <li>{@link TemporalPrecisionEnum#SECOND}
	 * <li>{@link TemporalPrecisionEnum#MILLI}
	 * </ul>
	 */
	@SimpleSetter
	public InstantDt(@SimpleSetter.Parameter(name = "theDate") Date theDate, @SimpleSetter.Parameter(name = "thePrecision") TemporalPrecisionEnum thePrecision) {
		setValue(theDate);
		setPrecision(thePrecision);
		setTimeZone(TimeZone.getDefault());
	}

	/**
	 * Create a new InstantDt from a string value
	 * 
	 * @param theString
	 *            The string representation of the string. Must be in a valid
	 *            format according to the FHIR specification
	 * @throws DataFormatException
	 */
	public InstantDt(String theString) {
		setValueAsString(theString);
	}

	/**
	 * Invokes {@link Date#after(Date)} on the contained Date against the given
	 * date
	 * 
	 * @throws NullPointerException
	 *             If the {@link #getValue() contained Date} is null
	 */
	public boolean after(Date theDate) {
		return getValue().after(theDate);
	}

	/**
	 * Invokes {@link Date#before(Date)} on the contained Date against the given
	 * date
	 * 
	 * @throws NullPointerException
	 *             If the {@link #getValue() contained Date} is null
	 */
	public boolean before(Date theDate) {
		return getValue().before(theDate);
	}

	/**
	 * Sets the value of this instant to the current time (from the system
	 * clock) and the local/default timezone (as retrieved using
	 * {@link TimeZone#getDefault()}. This TimeZone is generally obtained from
	 * the underlying OS.
	 */
	public void setToCurrentTimeInLocalTimeZone() {
		setValue(new Date());
		setTimeZone(TimeZone.getDefault());
	}

	@Override
	boolean isPrecisionAllowed(TemporalPrecisionEnum thePrecision) {
		switch (thePrecision) {
		case SECOND:
		case MILLI:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Factory method which creates a new InstantDt and initializes it with the
	 * current time.
	 */
	public static InstantDt withCurrentTime() {
		return new InstantDt(new Date());
	}

}

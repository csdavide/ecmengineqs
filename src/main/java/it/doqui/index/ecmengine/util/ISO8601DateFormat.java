/* Index ECM Engine - A system for managing the capture (when created
 * or received), classification (cataloguing), storage, retrieval,
 * revision, sharing, reuse and disposition of documents.
 *
 * Copyright (C) 2008 Regione Piemonte
 * Copyright (C) 2008 Provincia di Torino
 * Copyright (C) 2008 Comune di Torino
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package it.doqui.index.ecmengine.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * <p>
 * Classe di utilit&agrave; per la formattazione di date secondo lo standard
 * ISO8601.
 * </p>
 * <p>
 * Il formato &egrave; il seguente:
 * </p>
 *
 * <pre>
 *    sYYYY-MM-DDThh:mm:ss.sssTZD
 * </pre>
 * <p>
 * Significato:
 * </p>
 * <ul>
 * <li>{@code sYYYY}: anno in quattro cifre con un carattere opzionale indicante
 * il segno. Il segno negativo (-) indica gli anni precedenti all'anno 0,
 * l'assenza di segno o la presenza di un segno positivo (+) indicano gli anni
 * seguenti l'anno 0.</li>
 * <li>{@code MM}: il mese in due cifre (es: 01 = gennaio).</li>
 * <li>{@code DD}: il giorno del mese in due cifre (da 00 a 31).</li>
 * <li>{@code hh}: l'ora del giorno in due cifre (da 00 a 23).</li>
 * <li>{@code mm}: il minuto dell'ora in due cifre (da 00 a 59).</li>
 * <li>{@code ss.sss}: i secondi fino alla terza cifra decimale (da 00.000 a
 * 59.999).</li>
 * <li>{@code TZD}: l'indicatore di <i>timezone</i> (ed: Z per <i>Zulu</i>
 * (ovvero UTC) oppure l'offset da UTC nel formato +hh:mm or -hh:mm).</li>
 * </ul>
 *
 * @author DoQui
 */
public final class ISO8601DateFormat {

	/**
	 * Formatta un oggetto {@code Date} in formato ISO8601.
	 *
	 * @param isoDate L'oggetto {@code Date} da formattare.
	 * @return La stringa formattata secondo lo standard ISO8601.
	 */
	public static String format(Date isoDate) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(isoDate);

		StringBuffer formatted = new StringBuffer();
		padInt(formatted, calendar.get(Calendar.YEAR), 4);
		formatted.append('-');
		padInt(formatted, calendar.get(Calendar.MONTH) + 1, 2);
		formatted.append('-');
		padInt(formatted, calendar.get(Calendar.DAY_OF_MONTH), 2);
		formatted.append('T');
		padInt(formatted, calendar.get(Calendar.HOUR_OF_DAY), 2);
		formatted.append(':');
		padInt(formatted, calendar.get(Calendar.MINUTE), 2);
		formatted.append(':');
		padInt(formatted, calendar.get(Calendar.SECOND), 2);
		formatted.append('.');
		padInt(formatted, calendar.get(Calendar.MILLISECOND), 3);

		TimeZone tz = calendar.getTimeZone();
		int offset = tz.getOffset(calendar.getTimeInMillis());
		if (offset != 0) {
			int hours = Math.abs((offset / (60000)) / 60);
			int minutes = Math.abs((offset / (60000)) % 60);
			formatted.append((offset < 0) ? '-' : '+');
			padInt(formatted, hours, 2);
			formatted.append(':');
			padInt(formatted, minutes, 2);
		} else {
			formatted.append('Z');
		}
		return formatted.toString();
	}

	/**
	 * Formatta un oggetto {@code Date} in formato ISO8601, compatibile con il
	 * formato Solr 6.
	 *
	 * @param isoDate L'oggetto {@code Date} da formattare.
	 * @return La stringa formattata secondo lo standard ISO8601.
	 */
	public static String formatZ(Date isoDate) {
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(isoDate);

		// AF 08/09/2016: Modifica per le date di solr 6
		// Reimposta a GMT per avere la data che finisce con Z
		calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

		StringBuffer formatted = new StringBuffer();
		padInt(formatted, calendar.get(Calendar.YEAR), 4);
		formatted.append('-');
		padInt(formatted, calendar.get(Calendar.MONTH) + 1, 2);
		formatted.append('-');
		padInt(formatted, calendar.get(Calendar.DAY_OF_MONTH), 2);
		formatted.append('T');
		padInt(formatted, calendar.get(Calendar.HOUR_OF_DAY), 2);
		formatted.append(':');
		padInt(formatted, calendar.get(Calendar.MINUTE), 2);
		formatted.append(':');
		padInt(formatted, calendar.get(Calendar.SECOND), 2);
		formatted.append('.');
		padInt(formatted, calendar.get(Calendar.MILLISECOND), 3);

		formatted.append('Z');
		return formatted.toString();
	}

	/**
	 * Parsifica una data rappresentata da una stringa in formato standard ISO8601.
	 *
	 * @param isoDate La data da parsificare.
	 * @return L'oggetto {@code Date} risultante.
	 */
	public static Date parse_iso8601(String isoDate) {
		Date parsed = null;
		try {
			int offset = 0;

			// extract year
			int year = Integer.parseInt(isoDate.substring(offset, offset += 4));
			if (isoDate.charAt(offset) != '-') {
				throw new IndexOutOfBoundsException(
					"Expected '-' character but found '" + isoDate.charAt(offset) + "'");
			}

			// extract month
			int month = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
			if (isoDate.charAt(offset) != '-') {
				throw new IndexOutOfBoundsException(
					"Expected '-' character but found '" + isoDate.charAt(offset) + "'");
			}

			// extract day
			int day = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
			if (isoDate.charAt(offset) != 'T') {
				throw new IndexOutOfBoundsException(
					"Expected 'T' character but found '" + isoDate.charAt(offset) + "'");
			}

			// extract hours, minutes, seconds and milliseconds
			int hour = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
			if (isoDate.charAt(offset) != ':') {
				throw new IndexOutOfBoundsException(
					"Expected ':' character but found '" + isoDate.charAt(offset) + "'");
			}
			int minutes = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
			if (isoDate.charAt(offset) != ':') {
				throw new IndexOutOfBoundsException(
					"Expected ':' character but found '" + isoDate.charAt(offset) + "'");
			}
			int seconds = Integer.parseInt(isoDate.substring(offset += 1, offset += 2));
			if (isoDate.charAt(offset) != '.') {
				throw new IndexOutOfBoundsException(
					"Expected '.' character but found '" + isoDate.charAt(offset) + "'");
			}
			int milliseconds = Integer.parseInt(isoDate.substring(offset += 1, offset += 3));

			// extract timezone
			String timezoneId;
			final char timezoneIndicator = isoDate.charAt(offset);
			if (timezoneIndicator == '+' || timezoneIndicator == '-') {
				timezoneId = "GMT" + isoDate.substring(offset);
			} else if (timezoneIndicator == 'Z') {
				timezoneId = "GMT";
			} else {
				throw new IndexOutOfBoundsException("Invalid time zone designator: " + timezoneIndicator);
			}
			final TimeZone timezone = TimeZone.getTimeZone(timezoneId);
			if (!timezone.getID().equals(timezoneId)) {
				throw new IndexOutOfBoundsException();
			}

			// initialize Calendar object#
			// Note: always de-serialise from Gregorian Calendar
			final Calendar calendar = new GregorianCalendar(timezone);
			calendar.setLenient(false);
			calendar.set(Calendar.YEAR, year);
			calendar.set(Calendar.MONTH, month - 1);
			calendar.set(Calendar.DAY_OF_MONTH, day);
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minutes);
			calendar.set(Calendar.SECOND, seconds);
			calendar.set(Calendar.MILLISECOND, milliseconds);

			// extract the date
			parsed = calendar.getTime();
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Failed to parse date: " + isoDate, e);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Failed to parse date: " + isoDate, e);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Failed to parse date: " + isoDate, e);
		}
		return parsed;
	}

	public static Date parse(String isoDate) {
		Date toReturn = null;
		// "2017-02-01T00:00:00.000+10:00"
		int LEN_DATE = 10;
		int LEN_SECONDS = 19;
		int LEN_MILLIS = 23;
		// int LEN_TIMEZONE = 29;
		// String FMT_TZ = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"; //non supportato da java
		// 1.5.0

		try {

			toReturn = parse_iso8601(isoDate);

		} catch (Exception e) {
			int isodateLen = isoDate.length();
			// System.out.println(isodateLen);
			String FMT_DATE = "yyyy-MM-dd";
			String FMT_SECONDS = "yyyy-MM-dd'T'HH:mm:ss";
			String FMT_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSS";
			try {
				if (isodateLen == LEN_SECONDS) {
					SimpleDateFormat sdf1 = new SimpleDateFormat(FMT_SECONDS);
					toReturn = sdf1.parse(isoDate);
				} else if (isodateLen == LEN_MILLIS) {
					SimpleDateFormat sdf1 = new SimpleDateFormat(FMT_MILLIS);
					toReturn = sdf1.parse(isoDate);
				} else if (isodateLen == LEN_DATE) {
					SimpleDateFormat sdf1 = new SimpleDateFormat(FMT_DATE);
					toReturn = sdf1.parse(isoDate);
				}
			} catch (ParseException e1) {
				throw new IllegalArgumentException("Failed to parse date: " + isoDate, e);
			}
			if (toReturn == null) {
				throw new IllegalArgumentException("Failed to parse date: " + isoDate);
			}
		}
		return toReturn;
	}

	private static void padInt(StringBuffer buffer, int value, int length) {
		final String strValue = Integer.toString(value);
		for (int i = length - strValue.length(); i > 0; i--) {
			buffer.append('0');
		}
		buffer.append(strValue);
	}

	public static Date parseQuietly(String input) {
		Date toReturn = null;
		try {
			toReturn = parse(input);
		} catch (Exception e) {
			toReturn = null;
		}
		return toReturn;
	}
}

/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a utility class for translation between {@link java.util.Locale}'s
 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a> and
 * UMS' language files. See <a href="http://r12a.github.io/apps/subtags/">here
 * for subtag lookup</a>. If UMS languages are removed or added, this class needs
 * to be updated. The class is immutable.
 *
 * @author Nadahar
 * @since 5.2.3
 */

public final class Languages {

	private static final Logger LOGGER = LoggerFactory.getLogger(Languages.class);
	/**
	 * If the below list is changed, methods {@link #localeToLanguageCode()} and
	 * {@link #languageCodeToLanguageCode()} must be updated correspondingly.
	 */
	private final static String[] UMS_BCP47_CODES = {
		"af",      // Afrikaans
		"ar",      // Arabic
		"pt-BR",   // Brazilian Portuguese
		"bg",      // Bulgarian
		"ca",      // Catalan, Valencian
		"zh-Hans", // Chinese, Han (Simplified variant)
		"zh-Hant", // Chinese, Han (Traditional variant)
		"cs",      // Czech
		"da",      // Danish
		"nl",      // Dutch, Flemish
		"en-GB",   // English, United Kingdom
		"en-US",   // English, United States
		"fi",      // Finnish
		"fr",      // French
		"de",      // German
		"el",      // Greek, Modern
		"iw",      // Hebrew (Java prefers the deprecated "iw" to "he")
		"hu",      // Hungarian
		"is",      // Icelandic
		"it",      // Italian
		"ja",      // Japanese
		"ko",      // Korean
		"no",      // Norwegian
		"fa",      // Persian (Farsi)
		"pl",      // Polish
		"pt",      // Portuguese
		"ro",      // Romanian, Moldavian, Moldovan
		"ru",      // Russian
		"sr",      // Serbian (Cyrillic)
		"sk",      // Slovak
		"sl",      // Slovenian
		"es",      // Spanish, Castilian
		"sv",      // Swedish
		"tr",      // Turkish
		"uk",      // Ukrainian
		"vi",      // Vietnamese
	};

	private final static String[] UMS_LANGUAGES = new String[UMS_BCP47_CODES.length];
	/**
	 * This map is also used as a synchronization object for {@link #translationsStatistics},
	 * {@link #lastpreferredLocale} and {@link #sortedLanguages}
	 */
	private static HashMap<String, TranslationStatistics> translationsStatistics = new HashMap<>((int) Math.round(UMS_BCP47_CODES.length * 1.34));
	private static Locale lastpreferredLocale = null;
	private static List<LanguageEntry> sortedLanguages = new ArrayList<>();

	static {
		for (int i = 0; i < UMS_BCP47_CODES.length; i++) {
			UMS_LANGUAGES[i] = WordUtils.capitalize(Locale.forLanguageTag(UMS_BCP47_CODES[i]).getDisplayName(Locale.getDefault()));
		}
	}

	private static class TranslationStatistics {
		public String name;
		public int phrases;
		public int phrasesApproved;
		public int phrasesTranslated;
		public int words;
		public int wordsApproved;
		public int wordsTranslated;
		public int approved;
		public int translated;
	}

	private static class LanguageEntry {
		public String tag;
		public String name;
		public Locale locale = null;
		public int coveragePercent;
	}

	private static String localeToLanguageCode(Locale locale) {
		/*
		 * This might seem redundant, but a language can also contain a
		 * country/region and a variant. Stating that e.g language
		 * "ar" should return "ar" means that "messages_ar.properties"
		 * will be used for any country/region and variant of Arabic.
		 * This should be true until UMS contains multiple dialects of Arabic,
		 * in which case different codes would have to be returned for the
		 * different dialects.
		 */

		if (locale == null) {
			return null;
		}
		String languageCode = locale.getLanguage();
		if (languageCode != null && !languageCode.isEmpty()) {
			switch (languageCode) {
				case "en":
					if (locale.getCountry().equalsIgnoreCase("GB")) {
						return "en-GB";
					} else {
						return "en-US";
					}
				case "pt":
					if (locale.getCountry().equalsIgnoreCase("BR")) {
						return "pt-BR";
					} else {
						return "pt";
					}
				case "nb":
				case "nn":
					return "no";
				case "cmn":
				case "zh":
					if (locale.getScript().equalsIgnoreCase("Hans")) {
						return "zh-Hans";
					} else if (locale.getCountry().equalsIgnoreCase("CN") || locale.getCountry().equalsIgnoreCase("SG")) {
						return "zh-Hans";
					} else {
						return "zh-Hant";
					}
				default:
					return languageCode;
			}
		} else {
			return null;
		}
	}

	private static String languageCodeToLanguageCode(String languageCode) {
		/*
		 * Performs the same conversion as localeToLanguageCode() but from a
		 * language tag instead of a Locale.
		 */
		if (languageCode == null) {
			return null;
		} else if (languageCode.isEmpty()) {
			return "";
		}
		switch (languageCode.toLowerCase(Locale.US)) {
			case "en-gb":
				return "en-GB";
			case "pt-br":
				return "pt-BR";
			case "cmn-cn":
			case "cmn-sg":
			case "cmn-hans":
			case "zh-cn":
			case "zh-sg":
			case "zh-hans":
				return "zh-Hans";
			default:
				if (languageCode.indexOf("-") > 0) {
					languageCode = languageCode.substring(0, languageCode.indexOf("-"));
				}
				if (languageCode.equalsIgnoreCase("nb") || languageCode.equalsIgnoreCase("nn")) {
					return "no";
				} else if (languageCode.equalsIgnoreCase("cmn") || languageCode.equalsIgnoreCase("zh")) {
					return "zh-Hant";
				} else if (languageCode.equalsIgnoreCase("en")) {
					return "en-US";
				} else {
					return languageCode.toLowerCase(Locale.US);
				}
		}
	}

	private static void populateTranslationsStatistics() {
		if (translationsStatistics.size() < 1) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Languages.class.getResourceAsStream("/resources/languages.properties"), StandardCharsets.UTF_8))) {
				Pattern pattern = Pattern.compile("^\\s*(?!#)\\b([^\\.=][^=]+[^\\.=])=(.*[^\\s])\\s*$");
				String line;
				while ((line = reader.readLine()) != null) {
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						try {
							String[] path = matcher.group(1).split("\\.");
							TranslationStatistics translationStatistics;
							if (translationsStatistics.containsKey(path[0])) {
								translationStatistics = translationsStatistics.get(path[0]);
							} else {
								translationStatistics = new TranslationStatistics();
								translationsStatistics.put(path[0], translationStatistics);
							}
							if (path.length < 2) {
								LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
							} else if (path[1].equalsIgnoreCase("name")) {
								translationStatistics.name = matcher.group(2);
							} else if (path[1].equalsIgnoreCase("phrases")) {
								if (path.length < 3) {
									translationStatistics.phrases = Integer.parseInt(matcher.group(2));
								} else {
									switch (path[2].toLowerCase(Locale.US)) {
										case "approved":
											translationStatistics.phrasesApproved = Integer.parseInt(matcher.group(2));
											break;
										case "translated":
											translationStatistics.phrasesTranslated = Integer.parseInt(matcher.group(2));
											break;
										default:
											LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
									}
								}
							} else if (path[1].equalsIgnoreCase("words")) {
								if (path.length < 3) {
									translationStatistics.words = Integer.parseInt(matcher.group(2));
								} else {
									switch (path[2].toLowerCase(Locale.US)) {
										case "approved":
											translationStatistics.wordsApproved = Integer.parseInt(matcher.group(2));
											break;
										case "translated":
											translationStatistics.wordsTranslated = Integer.parseInt(matcher.group(2));
											break;
										default:
											LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
									}
								}
							} else if (path[1].equalsIgnoreCase("progress")) {
								if (path.length < 3) {
									LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
								} else {
									switch (path[2].toLowerCase(Locale.US)) {
										case "approved":
											translationStatistics.approved = Integer.parseInt(matcher.group(2));
											break;
										case "translated":
											translationStatistics.translated = Integer.parseInt(matcher.group(2));
											break;
										default:
											LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
									}
								}
							} else {
								LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
							}
						} catch (NumberFormatException e) {
							LOGGER.debug("Failed to parse translation statistics line \"{}\": ", line, e.getMessage());
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("Error reading translations statistics: {}", e.getMessage());
				LOGGER.trace("", e);
				translationsStatistics.clear();
			}
		}
	}

	/**
	 * Reads translations statistics from resource file <code>languages.properties</code>
	 * and returns them in a {@link HashMap} with language tags as keys.
	 * Results are cached for subsequent reads.
	 * <p>
	 * <strong>The returned {@link HashMap} is never <code>null</code> must
	 * always be synchronized on itself during read or write</strong>
	 * @return The resulting {@link HashMap}
	 */
	public static HashMap<String, TranslationStatistics> getTranslationsStatistics() {
		synchronized (translationsStatistics) {
			populateTranslationsStatistics();
			return translationsStatistics;
		}
	}

	/**
	 * Verifies if a given <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag is supported by UMS.
	 * @param languageCode The language tag in IEFT BCP 47 format.
	 * @return The result.
	 */
	public static boolean isValid(String languageCode) {
		if (languageCode != null && !languageCode.isEmpty()) {
			for (String code : UMS_BCP47_CODES) {
				if (code.equalsIgnoreCase(languageCode)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Verifies if a given {@link java.util.Locale} is supported by UMS.
	 * @param locale The {@link java.util.Locale}.
	 * @return The result.
	 */
	public static boolean isValid(Locale locale) {
		return isValid(localeToLanguageCode(locale));
	}

	/**
	 * Verifies if a given <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag is or can be converted into a language tag supported by UMS.
	 * @param languageCode The language tag in IEFT BCP 47 format.
	 * @return The result.
	 */
	public static boolean isCompatible(String languageCode) {
		return isValid(languageCodeToLanguageCode(languageCode));
	}

	/** Returns a correctly capitalized <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 *  language tag if the language tag is supported by UMS, or returns null.
	 * @param languageCode The IEFT BCP 47 compatible language tag.
	 * @return The IEFT BCP 47 formatted language tag.
	 */
	public static String toLanguageCode(String languageCode) {
		if (languageCode != null && !languageCode.isEmpty()) {
			languageCode = languageCodeToLanguageCode(languageCode);
			for (String code : UMS_BCP47_CODES) {
				if (code.equalsIgnoreCase(languageCode)) {
					return code;
				}
			}
		}
		return null;
	}

	/** Returns a correctly capitalized <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 *  language tag if the language tag is supported by UMS, or returns null.
	 * @param locale The {@link java.util.Locale}.
	 * @return The IEFT BCP 47 formatted language tag.
	 */
	public static String toLanguageCode(Locale locale) {
		if (locale != null) {
			return toLanguageCode(localeToLanguageCode(locale));
		}
		return null;
	}

	/**
	 * Returns a UMS supported {@link java.util.Locale} from the given
	 * <code>Local</code> if it can be found (<code>en</code> is translated to
	 * <code>en-US</code>, <code>zh</code> to <code>zh-Hant</code> etc.).
	 * Returns <code>null</code> if a valid <code>Locale</code> cannot be found.
	 * @param locale Source {@link java.util.Locale}.
	 * @return Resulting {@link java.util.Locale}.
	 */
	public static Locale toLocale(Locale locale) {
		if (locale != null) {
			String code = localeToLanguageCode(locale);
			if (code != null && isValid(code)) {
				return Locale.forLanguageTag(code);
			}
		}
		return null;
	}

	/**
	 * Returns a UMS supported {@link java.util.Locale} from the given
	 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * if it can be found (<code>en</code> is translated to <code>en-US</code>,
	 * <code>zh</code> to <code>zh-Hant</code> etc.). Returns <code>null</code>
	 * if a valid <code>Locale</code> cannot be found.
	 * @param locale Source {@link java.util.Locale}.
	 * @return Resulting {@link java.util.Locale}.
	 */
	public static Locale toLocale(String languageCode) {
		if (languageCode != null) {
			String code = languageCodeToLanguageCode(languageCode);
			if (isValid(code)) {
				return Locale.forLanguageTag(code);
			}
		}
		return null;
	}

	/**
	 * This method must be called in a context synchronized on {@link #translationsStatistics}.
	 */
	private static void createSortedList(Locale preferredLocale) {
		if (preferredLocale == null) {
			throw new IllegalArgumentException("preferredLocale cannot be null");
		}
		//TODO: move synch out
		synchronized(translationsStatistics) {
		if (lastpreferredLocale == null || !lastpreferredLocale.equals(preferredLocale)) {
			lastpreferredLocale = (Locale) preferredLocale.clone();
			sortedLanguages.clear();
			populateTranslationsStatistics();
			for (String tag : UMS_BCP47_CODES) {
				LanguageEntry entry = new LanguageEntry();
				entry.tag = tag;
				entry.name = Messages.getString("Language." + tag, preferredLocale);
				if (!entry.name.equals(Messages.getRootString("Language." + tag))) {
					entry.name += " (" + Messages.getRootString("Language." + tag) + ")";
				}
				entry.locale = Locale.forLanguageTag(tag);
				TranslationStatistics stats = translationsStatistics.get(tag);
				if (stats != null) {
					entry.coveragePercent = stats.translated;
				} else {
					entry.coveragePercent = 0;
					LOGGER.debug("Warning: Could not find language statistics for {}", entry.name);
				}
			}
		}
		}
	}

	public static String[] getLanguageTags() {
		return UMS_BCP47_CODES;
	}

	public static String[] getLanguageNames() {
		return UMS_LANGUAGES;
	}
}

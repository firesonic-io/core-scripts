package support

import ca.uhn.hl7v2.DefaultHapiContext
import ca.uhn.hl7v2.HapiContext
import ca.uhn.hl7v2.model.Message
import ca.uhn.hl7v2.parser.Parser
import ca.uhn.hl7v2.util.Terser
import io.firesonic.script.api.MessageEnvelope

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Test-data builder for {@link MessageEnvelope}.
 *
 * <p>
 * The builder can create an envelope from the built-in default HL7 message,
 * from a custom raw HL7 string, or from an HL7 file placed under
 * {@code src/test/resources}.
 * </p>
 *
 * <p>
 * During {@link #build()}, the raw message is normalized and parsed. Unless a
 * value was explicitly supplied through a setter, the following envelope
 * fields are derived from MSH:
 * </p>
 *
 * <ul>
 * <li>messageCode from MSH-9.1</li>
 * <li>triggerEvent from MSH-9.2</li>
 * <li>messageControlId from MSH-10</li>
 * </ul>
 *
 * <p>
 * Value precedence is:
 * </p>
 *
 * <pre>
 * explicit builder setter -> value derived from MSH -> fallback test value
 * </pre>
 */
final class MessageEnvelopeBuilder {

	private static final String DEFAULT_MESSAGE_CODE = 'ADT'
	private static final String DEFAULT_TRIGGER_EVENT = 'A01'
	private static final String DEFAULT_MESSAGE_CONTROL_ID = 'MSG-0001'

	private static final String DEFAULT_RAW_HL7 =
	'MSH|^~\\&|TEST-SENDER|TEST-FACILITY|FireSonic|TEST|' +
	'20260101000000||ADT^A01|MSG-0001|P|2.5\r' +
	'EVN|A01|20260101000000\r' +
	'PID|1||123456^^^HOSPITAL^MR||Miller^Emma\r' +
	'PV1|1|I\r'

	private static final HapiContext HAPI_CONTEXT = new DefaultHapiContext()
	private static final Parser DEFAULT_PARSER = HAPI_CONTEXT.getPipeParser()

	private String id = UUID.randomUUID().toString()
	private String rawHl7 = DEFAULT_RAW_HL7
	private Message hl7Message

	private String messageCode
	private String triggerEvent
	private String messageControlId

	private boolean messageCodeExplicitlySet
	private boolean triggerEventExplicitlySet
	private boolean messageControlIdExplicitlySet

	private InetSocketAddress remoteAddress =
	new InetSocketAddress('127.0.0.1', 7575)

	private Instant receivedAt =
	Instant.parse('2026-01-01T00:00:00Z')

	private Parser parser = DEFAULT_PARSER
	private boolean parseRawHl7 = true
	private Charset resourceCharset = StandardCharsets.UTF_8

	MessageEnvelopeBuilder setId(String id) {
		this.id = id
		return this
	}

	/**
	 * Explicitly overrides the message code that would otherwise be read from
	 * MSH-9.1.
	 */
	MessageEnvelopeBuilder setMessageCode(String messageCode) {
		this.messageCode = messageCode
		this.messageCodeExplicitlySet = true
		return this
	}

	/**
	 * Explicitly overrides the trigger event that would otherwise be read from
	 * MSH-9.2.
	 */
	MessageEnvelopeBuilder setTriggerEvent(String triggerEvent) {
		this.triggerEvent = triggerEvent
		this.triggerEventExplicitlySet = true
		return this
	}

	/**
	 * Explicitly overrides the message control id that would otherwise be read
	 * from MSH-10.
	 */
	MessageEnvelopeBuilder setMessageControlId(String messageControlId) {
		this.messageControlId = messageControlId
		this.messageControlIdExplicitlySet = true
		return this
	}

	/**
	 * Sets a raw HL7 message. The parsed message is cleared and will be recreated
	 * during build() when automatic parsing is enabled.
	 */
	MessageEnvelopeBuilder setRawHl7(String rawHl7) {
		this.rawHl7 = rawHl7
		this.hl7Message = null
		return this
	}

	/**
	 * Uses an already parsed HAPI HL7 message. When supplied, this instance takes
	 * precedence over automatic parsing of rawHl7.
	 */
	MessageEnvelopeBuilder setHl7Message(Message hl7Message) {
		this.hl7Message = hl7Message
		return this
	}

	MessageEnvelopeBuilder setRemoteAddress(
			InetSocketAddress remoteAddress) {

		this.remoteAddress = remoteAddress
		return this
	}

	MessageEnvelopeBuilder setReceivedAt(Instant receivedAt) {
		this.receivedAt = receivedAt
		return this
	}

	/**
	 * Allows a test to provide a custom HAPI parser.
	 */
	MessageEnvelopeBuilder setParser(Parser parser) {
		this.parser = Objects.requireNonNull(parser, 'parser')
		return this
	}

	/**
	 * Sets the character encoding used when loading HL7 files from the test
	 * classpath. UTF-8 is used by default.
	 */
	MessageEnvelopeBuilder setResourceCharset(Charset resourceCharset) {
		this.resourceCharset = Objects.requireNonNull(
				resourceCharset,
				'resourceCharset'
				)
		return this
	}

	/**
	 * Disables automatic parsing when a test intentionally needs
	 * MessageEnvelope.hl7Message to be null.
	 */
	MessageEnvelopeBuilder setParseRawHl7(boolean parseRawHl7) {
		this.parseRawHl7 = parseRawHl7
		return this
	}

	/**
	 * Loads an HL7 message from the test classpath.
	 *
	 * <p>
	 * Files normally reside under {@code src/test/resources}. The supplied path
	 * is classpath-relative and may optionally start with {@code /}.
	 * </p>
	 *
	 * <pre>
	 * TestFixtures.envelope()
	 *     .fromResource('hl7/adt-a01-with-multiple-al1.hl7')
	 *     .build()
	 * </pre>
	 */
	MessageEnvelopeBuilder fromResource(String resourcePath) {
		String normalizedPath = normalizeResourcePath(resourcePath)
		ClassLoader loader = resolveClassLoader()

		InputStream stream = loader.getResourceAsStream(normalizedPath)
		if (stream == null) {
			throw new IllegalArgumentException(
			'HL7 test resource not found on classpath: ' + normalizedPath
			)
		}

		try {
			String loaded = stream.getText(resourceCharset.name())
			setRawHl7(removeUtf8Bom(loaded))
		} finally {
			stream.close()
		}

		return this
	}

	/**
	 * Alias for {@link #fromResource(String)}.
	 */
	MessageEnvelopeBuilder resourceFile(String resourcePath) {
		return fromResource(resourcePath)
	}

	MessageEnvelope build() {
		Objects.requireNonNull(id, 'id')
		Objects.requireNonNull(rawHl7, 'rawHl7')
		Objects.requireNonNull(receivedAt, 'receivedAt')

		String normalizedRawHl7 = normalizeLineEndings(rawHl7)
		Message effectiveHl7Message = resolveHl7Message(normalizedRawHl7)
		EnvelopeMetadata metadata = resolveMetadata(effectiveHl7Message)

		String effectiveMessageCode = messageCodeExplicitlySet
				? messageCode
				: firstNonBlank(metadata.messageCode, DEFAULT_MESSAGE_CODE)

		String effectiveTriggerEvent = triggerEventExplicitlySet
				? triggerEvent
				: firstNonBlank(metadata.triggerEvent, DEFAULT_TRIGGER_EVENT)

		String effectiveMessageControlId = messageControlIdExplicitlySet
				? messageControlId
				: firstNonBlank(metadata.messageControlId, DEFAULT_MESSAGE_CONTROL_ID)

		return new MessageEnvelope(
				id,
				effectiveMessageCode,
				effectiveTriggerEvent,
				effectiveMessageControlId,
				normalizedRawHl7,
				effectiveHl7Message,
				remoteAddress,
				receivedAt
				)
	}

	private Message resolveHl7Message(String normalizedRawHl7) {
		if (hl7Message != null) {
			return hl7Message
		}

		if (!parseRawHl7 || normalizedRawHl7.isBlank()) {
			return null
		}

		try {
			return parser.parse(normalizedRawHl7)
		} catch (Exception ex) {
			throw new IllegalStateException(
			'Failed to parse test HL7 message',
			ex
			)
		}
	}

	private static EnvelopeMetadata resolveMetadata(Message message) {
		if (message == null) {
			return EnvelopeMetadata.EMPTY
		}

		try {
			Terser terser = new Terser(message)

			return new EnvelopeMetadata(
					trimToNull(terser.get('/MSH-9-1')),
					trimToNull(terser.get('/MSH-9-2')),
					trimToNull(terser.get('/MSH-10'))
					)
		} catch (Exception ex) {
			throw new IllegalStateException(
			'Failed to extract envelope metadata from test HL7 message',
			ex
			)
		}
	}

	private static ClassLoader resolveClassLoader() {
		ClassLoader contextLoader = Thread.currentThread().getContextClassLoader()
		return contextLoader != null
				? contextLoader
				: MessageEnvelopeBuilder.class.getClassLoader()
	}

	private static String normalizeResourcePath(String resourcePath) {
		if (resourcePath == null || resourcePath.isBlank()) {
			throw new IllegalArgumentException('resourcePath must not be blank')
		}

		String normalized = resourcePath.trim().replace('\\', '/')
		while (normalized.startsWith('/')) {
			normalized = normalized.substring(1)
		}

		if (normalized.isBlank()) {
			throw new IllegalArgumentException('resourcePath must not be blank')
		}

		return normalized
	}

	private static String normalizeLineEndings(String value) {
		return value
				.replace('\r\n', '\r')
				.replace('\n', '\r')
	}

	private static String removeUtf8Bom(String value) {
		return value != null && value.startsWith('\uFEFF')
				? value.substring(1)
				: value
	}

	private static String firstNonBlank(String value, String fallback) {
		return value != null && !value.isBlank()
				? value.trim()
				: fallback
	}

	private static String trimToNull(String value) {
		return value == null || value.isBlank()
				? null
				: value.trim()
	}

	private static final class EnvelopeMetadata {

		private static final EnvelopeMetadata EMPTY =
		new EnvelopeMetadata(null, null, null)

		final String messageCode
		final String triggerEvent
		final String messageControlId

		EnvelopeMetadata(
		String messageCode,
		String triggerEvent,
		String messageControlId) {

			this.messageCode = messageCode
			this.triggerEvent = triggerEvent
			this.messageControlId = messageControlId
		}
	}
}

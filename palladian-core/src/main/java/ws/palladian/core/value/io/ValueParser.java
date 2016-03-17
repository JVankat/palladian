package ws.palladian.core.value.io;

import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableLongValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.Value;

/**
 * Defines a parser for transforming strings to appropriate {@link Value}s.
 * 
 * @author pk
 * @see ImmutableStringValue#PARSER
 * @see ImmutableDoubleValue#PARSER
 * @see ImmutableLongValue#PARSER
 */
public interface ValueParser {

	/**
	 * Parse the given input value.
	 * 
	 * @param input
	 *            The input value.
	 * @return The parsed input value.
	 * @throws ValueParserException
	 *             in case the value could not be parsed.
	 */
	Value parse(String input) throws ValueParserException;

	/**
	 * Determine, whether the given input value can be parsed.
	 * 
	 * @param input
	 *            The input value.
	 * @return <code>true</code> in case this parser can handle the given value.
	 */
	boolean canParse(String input);

}

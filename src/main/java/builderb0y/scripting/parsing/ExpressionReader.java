package builderb0y.scripting.parsing;

import com.mojang.brigadier.StringReader;

import net.minecraft.util.function.CharPredicate;

/**
similar to {@link StringReader}, but with some additional functionality thrown in,
and some unnecessary functionality thrown out.
*/
public class ExpressionReader {

	public final String input;
	public int cursor, line, column;

	public ExpressionReader(String input) {
		this.input = canonicalizeLineEndings(input);
		this.line = 1;
		this.column = 1;
	}

	/** replaces "\r" and "\r\n" with "\n". */
	public static String canonicalizeLineEndings(String input) {
		int length = input.length();
		StringBuilder builder = new StringBuilder(length);
		for (int index = 0; index < length;) {
			char c = input.charAt(index);
			if (c == '\r') { //canonicalize line endings.
				index++;
				if (index < length && input.charAt(index) == '\n') {
					index++;
				}
				builder.append('\n');
			}
			else {
				builder.append(c);
				index++;
			}
		}
		return builder.toString();
	}

	public char getChar(int index) {
		return index < this.input.length() ? this.input.charAt(index) : 0;
	}

	public boolean canRead() {
		return this.cursor < this.input.length();
	}

	public boolean canReadAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.canRead();
	}

	/**
	called after a char is read. updates {@link #cursor}, {@link #line},
	and {@link #column} based on the character which was read.
	*/
	public void onCharRead(char c) {
		this.cursor++;
		if (c == '\n') {
			this.line++;
			this.column = 1;
		}
		else {
			this.column++;
		}
	}

	/**
	called after multiple chars are read. updates {@link #cursor}, {@link #line},
	and {@link #column} based on the characters which were read.
	*/
	public void onCharsRead(String s) {
		for (int index = 0, length = s.length(); index < length; index++) {
			this.onCharRead(s.charAt(index));
		}
	}

	/**
	reads one character, and returns that character.
	returns 0 ('\0' or '\u0000') if there are no more
	characters to read because we reached the end of the input.
	{@link #cursor}, {@link #line}, and {@link #column}
	are updated based on the character which was read.
	*/
	public char read() throws ScriptParsingException {
		if (this.canRead()) {
			char c = this.input.charAt(this.cursor);
			this.onCharRead(c);
			if (c == 0) {
				throw new ScriptParsingException("Encountered NUL character in input stream", this);
			}
			return c;
		}
		return 0;
	}

	public char readAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.read();
	}

	/**
	returns the next character which would be read by {@link #read()},
	without actually reading it. {@link #cursor}, {@link #line}, and
	{@link #column} are left unchanged as a result of calling this method.
	*/
	public char peek() {
		return this.canRead() ? this.input.charAt(this.cursor) : 0;
	}

	public char peekAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peek();
	}

	/**
	attempts to read one character, returns true if successful,
	and false if we reached the end of our input.
	*/
	public boolean skip() throws ScriptParsingException {
		return this.read() != 0;
	}

	/**
	attempts to skip (count) characters.
	stops if the end of input has been reached,
	or is reached in the process of skipping.
	returns the number of characters which were actually skipped.
	the returned number will be less than (count)
	if the end of input was reached while skipping.
	*/
	public int skip(int count) {
		count = Math.min(count, this.input.length() - this.cursor);
		for (int i = 0; i < count; i++) {
			this.onCharRead(this.input.charAt(this.cursor));
		}
		return count;
	}

	/**
	attempts to read (count) characters.
	stops if the end of input has been reached,
	or is reached in the process of reading.
	returns the characters that were read, as a String.
	the returned String's {@link String#length()} will be less
	than (count) if the end of input was reached while reading.
	*/
	public String read(int count) {
		int startIndex = this.cursor;
		int endIndex = Math.min(startIndex + count, this.input.length());
		String read = this.input.substring(startIndex, endIndex);
		this.onCharsRead(read);
		return read;
	}

	/**
	if the next character to be read is (expected),
	reads that character, and returns true.
	otherwise, does NOT read that character, and returns false.
	*/
	public boolean has(char expected) {
		if (!this.canRead()) return false;
		char got = this.input.charAt(this.cursor);
		if (got != expected) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(char expected) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(expected);
	}

	/**
	if the next characters to be read are (expected),
	reads those characters and returns true.
	otherwise, does NOT read those characters, and returns false.
	*/
	public boolean has(String expected) {
		if (this.input.regionMatches(this.cursor, expected, 0, expected.length())) {
			this.onCharsRead(expected);
			return true;
		}
		return false;
	}

	public boolean hasAfterWhitespace(String expected) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(expected);
	}

	/**
	if the next character to be read tests true by the
	provided predicate, reads that character and returns true.
	otherwise, does NOT read that character, and returns false.
	*/
	public boolean has(CharPredicate predicate) {
		if (!this.canRead()) return false;
		char got = this.input.charAt(this.cursor);
		if (!predicate.test(got)) return false;
		this.onCharRead(got);
		return true;
	}

	public boolean hasAfterWhitespace(CharPredicate predicate) throws ScriptParsingException {
		this.skipWhitespace();
		return this.has(predicate);
	}

	/**
	while the next character to be read tests true
	by th provided predicate, skips that character.
	*/
	public void skipWhile(CharPredicate predicate) {
		char c;
		while (this.canRead() && predicate.test(c = this.input.charAt(this.cursor))) {
			this.onCharRead(c);
		}
	}

	/**
	while the next character to be read tests true
	by th provided predicate, reads that character.
	returns the characters which were read.
	*/
	public String readWhile(CharPredicate predicate) {
		int start = this.cursor;
		this.skipWhile(predicate);
		return this.input.substring(start, this.cursor);
	}

	/**
	skips whitespace, and comments.
	comments are defined as starting with a semicolon,
	and ending with a newline character.
	this method should only be called
	outside of a string literal context.
	*/
	public void skipWhitespace() throws ScriptParsingException {
		while (true) {
			this.skipWhile(Character::isWhitespace);
			if (this.has(';')) {
				boolean multiline = this.has(';');
				int end;
				if (multiline) {
					end = this.input.indexOf(";;", this.cursor);
					if (end < 0) throw new ScriptParsingException("Un-terminated multi-line comment", this);
					this.skip(end + 2 - this.cursor);
				}
				else {
					end = this.input.indexOf('\n', this.cursor);
					if (end < 0) end = this.input.length();
					this.skip(end + 1 - this.cursor);
				}
			}
			else {
				break;
			}
		}
	}

	/**
	if the next character to be read is (expected),
	reads that character and returns normally.
	otherwise, does NOT read that character,
	and throws a {@link ScriptParsingException}.
	*/
	public void expect(char expected) throws ScriptParsingException {
		if (!this.has(expected)) {
			throw new ScriptParsingException("Expected '" + expected + '\'', this);
		}
	}

	public void expectAfterWhitespace(char expected) throws ScriptParsingException {
		this.skipWhitespace();
		this.expect(expected);
	}

	/**
	if the next characters to be read are (expected),
	reads those characters and returns normally.
	otherwise, does NOT read those characters,
	and throws a {@link ScriptParsingException}.
	*/
	public void expect(String expected) throws ScriptParsingException {
		if (!this.has(expected)) {
			throw new ScriptParsingException("Expected '" + expected + '\'', this);
		}
	}

	public void expectAfterWhitespace(String expected) throws ScriptParsingException {
		this.skipWhitespace();
		this.expect(expected);
	}

	/**
	reads an operator as if by {@link #readOperator()},
	but does not modify {@link #cursor}, {@link #line}, or {@link #column}.
	*/
	public String peekOperator() {
		CursorPos old = this.getCursor();
		String operator = this.readOperator();
		this.setCursor(old);
		return operator;
	}

	public String peekOperatorAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peekOperator();
	}

	/**
	reads an operator.
	operators are defined as one or more operator symbols,
	see {@link #isOperatorSymbol(char)} for the exact predicate used
	to determine whether or not a character is an operator symbol.
	*/
	public String readOperator() {
		return this.readWhile(ExpressionReader::isOperatorSymbol);
	}

	public String readOperatorAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.readOperator();
	}

	/**
	returns true if the provided character counts as an operator character.
	operator characters include the following:
		!#%&*+,-./:;<=>?@\^|~
	*/
	public static boolean isOperatorSymbol(char c) {
		return switch (c) {
			case '!', '#', '%', '&', '*', '+', ',', '-', '.', '/', ':', ';', '<', '=', '>', '?', '@', '\\', '^', '|', '~' -> true;
			default -> false;
		};
	}

	public boolean hasOperator(String operator) {
		CursorPos revert = this.getCursor();
		if (this.has(operator) && !isOperatorSymbol(this.peek())) {
			return true;
		}
		else {
			this.setCursor(revert);
			return false;
		}
	}

	public boolean hasOperatorAfterWhitespace(String operator) throws ScriptParsingException {
		this.skipWhitespace();
		return this.hasOperator(operator);
	}

	public void expectOperator(String operator) throws ScriptParsingException {
		if (!this.hasOperator(operator)) throw new ScriptParsingException("Expected '" + operator + '\'', this);
	}

	public void expectOperatorAfterWhitespace(String operator) throws ScriptParsingException {
		this.skipWhitespace();
		this.expectOperator(operator);
	}

	public static boolean isLetterOrUnderscore(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c == '_');
	}

	public static boolean isLetterNumberOrUnderscore(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_');
	}

	public String readIdentifier() throws ScriptParsingException {
		char c = this.peek();
		if (isLetterOrUnderscore(c)) {
			int startIndex = this.cursor;
			do {
				this.onCharRead(c);
				c = this.peek();
			}
			while (isLetterNumberOrUnderscore(c));
			return this.input.substring(startIndex, this.cursor);
		}
		else if (c == '`') {
			this.onCharRead('`');
			CursorPos start = this.getCursor();
			while (true) {
				c = this.peek();
				if (c == 0) {
					this.setCursor(start);
					throw new ScriptParsingException("Un-terminated escaped identifier", this);
				}
				if (c == '`') {
					this.onCharRead('`');
					break;
				}
				this.onCharRead(c);
			}
			return this.input.substring(start.cursor, this.cursor - 1);
		}
		else {
			return "";
		}
	}

	public String readIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.readIdentifier();
	}

	public String peekIdentifier() throws ScriptParsingException {
		CursorPos revert = this.getCursor();
		String identifier = this.readIdentifier();
		this.setCursor(revert);
		return identifier;
	}

	public String peekIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.peekIdentifier();
	}

	public boolean hasIdentifier(String identifier) throws ScriptParsingException {
		char c = this.peek();
		if (isLetterOrUnderscore(c)) {
			if (
				this.input.regionMatches(this.cursor, identifier, 0, identifier.length()) &&
				!isLetterNumberOrUnderscore(this.getChar(this.cursor + identifier.length()))
			) {
				this.onCharsRead(identifier);
				return true;
			}
		}
		else if (c == '`') {
			if (
				this.input.regionMatches(this.cursor + 1, identifier, 0, identifier.length()) &&
				this.getChar(this.cursor + identifier.length() + 2) == '`'
			) {
				this.onCharRead('`');
				this.onCharsRead(identifier);
				this.onCharRead('`');
				return true;
			}
		}
		return false;
	}

	public boolean hasIdentifierAfterWhitespace(String identifier) throws ScriptParsingException {
		this.skipWhitespace();
		return this.hasIdentifier(identifier);
	}

	public String expectIdentifier() throws ScriptParsingException {
		String identifier = this.readIdentifier();
		if (!identifier.isEmpty()) return identifier;
		else throw new ScriptParsingException("Expected identifier", this);
	}

	public String expectIdentifierAfterWhitespace() throws ScriptParsingException {
		this.skipWhitespace();
		return this.expectIdentifier();
	}

	public void expectIdentifier(String identifier) throws ScriptParsingException {
		if (!this.hasIdentifier(identifier)) {
			throw new ScriptParsingException("Expected '" + identifier + '\'', this);
		}
	}

	public void expectIdentifierAfterWhitespace(String identifier) throws ScriptParsingException {
		this.skipWhitespace();
		this.expectIdentifier(identifier);
	}

	public String getSource() {
		return this.input;
	}

	public String getSourceForError() {
		return this.input.substring(0, this.cursor);
	}

	public void unread() {
		int newCursor = this.cursor - 1;
		char c = this.input.charAt(newCursor);
		if (c == '\n') throw new IllegalStateException("Cannot unread newline");
		this.cursor = newCursor;
		this.column--;
	}

	public CursorPos getCursor() {
		return new CursorPos(this.cursor, this.line, this.column);
	}

	public void setCursor(CursorPos position) {
		this.cursor = position.cursor;
		this.line   = position.line  ;
		this.column = position.column;
	}

	public static record CursorPos(int cursor, int line, int column) {}
}
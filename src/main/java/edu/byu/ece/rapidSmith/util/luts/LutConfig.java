package edu.byu.ece.rapidSmith.util.luts;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.*;

/**
 *
 */
public final class LutConfig {
	private String operatingMode;
	private String output;
	private boolean isVcc;
	private boolean isGnd;
	private LutContents contents;

	public LutConfig() {}

	public LutConfig(String cfgString) {
		parseLutAttribute(cfgString);
	}

	private void parseLutAttribute(String attr) {
		// prep the parser
		ANTLRInputStream input = new ANTLRInputStream(attr);
		LutEquationLexer lexer = new LutEquationLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		LutEquationParser parser = new LutEquationParser(tokens);
		// Replace the default error listener with my fail hard listener
		parser.removeErrorListeners();
		parser.addErrorListener(new LutParseErrorListener());

		// do the actual parsing
		ParseTree tree = parser.config_string();

		// traverse the tree
		LutParserListener listener = new LutParserListener();
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(listener, tree);
	}

	/**
	 * Returns the operating mode of this LUT, eg LUT, RAM, ROM.
	 *
	 * @return the operating mode of this LUT
	 */
	public String getOperatingMode() {
		return operatingMode;
	}

	/**
	 * Sets the operating mode of this LUT, eg LUT, RAM, ROM.
	 *
	 * @param operatingMode the new operating mode
	 */
	public void setOperatingMode(String operatingMode) {
		this.operatingMode = operatingMode;
	}

	/**
	 * Name of the output pin for this LUT, eg O5, O6.
	 *
	 * @return the output pin for this LUT
	 */
	public String getOutputPinName() {
		return output;
	}

	/**
	 * Sets the output pin for this LUT, eg O5, O6.
	 *
	 * @param output the new output pin
	 */
	public void setOutputPinName(String output) {
		this.output = output;
	}

	/**
	 * Returns true if this LUT is configured as a static source.  A LUT is
	 * considered a static source if it is configured in LUT operating mode and
	 * its equation is "0" or "1".
	 *
	 * @return true if this LUT is configured as a static source
	 */
	public boolean isStaticSource() {
		return isVccSource() || isGndSource();
	}

	/**
	 * Returns true if this LUT is configured as a VCC source.  A LUT is
	 * considered a VCC source if it is configured in LUT operating mode and its
	 * equations is "1".
	 *
	 * @return true if this LUT is configured as a VCC source
	 */
	public boolean isVccSource() {
		return isVcc;
	}

	/**
	 * Returns true if this LUT is configured as a ground source.  A LUT is
	 * considered a ground source if it is configured in LUT operating mode and its
	 * equations is "0".
	 *
	 * @return true if this LUT is configured as a ground source
	 */
	public boolean isGndSource() {
		return isGnd;
	}

	/**
	 * Returns the configuration of this LUT in tree form.  If needed, the tree will
	 * be constructed from the initString.  Returns null if the LUT is a static
	 * source.
	 *
	 * @return the contents of this LUT
	 */
	public LutContents getContents() {
		return contents;
	}

	public void configureAsVccSource() {
		reset();
		this.isVcc = true;
	}

	public void configureAsGndSource() {
		reset();
		this.isGnd = true;
	}

	public void setContents(LutContents contents) {
		reset();
		this.contents = contents;
	}

	private void reset() {
		isVcc = false;
		isGnd = false;
		contents = null;
	}

	/**
	 * Computes and returns the number of inputs required by this LUT's
	 * configuration.  For example, ((A6 * ~A6) + A5) returns 2.
	 *
	 * @return the number of inputs required by this LUT's configuration
	 */
	public int getNumUsedInputs() {
		if (isStaticSource()) // contents are null when configured as static
			return 0;
		return contents.getUsedInputs().size();
	}

	public Set<Integer> getUsedInputs() {
		if (isStaticSource())
			return Collections.emptySet();
		return contents.getUsedInputs();
	}

	/**
	 * Computes and returns the minimum number of inputs required by this LUT's
	 * configuration once the equation is optimized.  For example, ((A6 * ~A6) + A5)
	 * returns 1.
	 *
	 * @return the number of inputs required by this LUT's configuration
	 */
	public int getMinNumOfInputs() {
		if (isStaticSource()) // contents are null when configured as static
			return 0;
		return contents.getMinNumOfInputs();
	}

	public LutConfig deepCopy() {
		LutConfig copy = new LutConfig();
		copy.operatingMode = operatingMode;
		copy.output = output;
		copy.isVcc = isVcc;
		copy.isGnd = isGnd;
		if (contents != null)
			copy.contents = contents.deepCopy();
		return copy;
	}

	public void remapPins(int[] remap) {
		Stack<LutEquation> stack = new Stack<>();
		LutEquation eqn = contents.getEquation();
		stack.push(eqn);
		while (!stack.isEmpty()) {
			LutEquation tree = stack.pop();
			if (tree.getClass() == LutInput.class) {
				LutInput lutInput = (LutInput) tree;
				int orig = lutInput.getIndex();
				// subtract 1 from orig to align with zero base array
				lutInput.setIndex(remap[orig-1]);
			} else if (tree.getClass() == BinaryOperation.class){
				BinaryOperation op = (BinaryOperation) tree;
				stack.push(op.getLeft());
				stack.push(op.getRight());
			} else if (tree.getClass() == Constant.class) {
				// do nothing
			} else {
				assert false : "Unknown tree node type";
			}
		}

		setContents(new LutContents(eqn));
	}

	public void reduce() {
		if (isStaticSource()) // contents are null when configured as static
			return;
		setContents(getContents().getReducedForm());
	}

	/* Parse a lut configuration attribute value to a LUTConfiguration */
	private class LutParserListener extends LutEquationBaseListener {

		@Override
		public void enterOp_mode(LutEquationParser.Op_modeContext ctx) {
			setOperatingMode(ctx.getText());
		}

		@Override
		public void enterOutput_pin(LutEquationParser.Output_pinContext ctx) {
			setOutputPinName(ctx.getText());
		}

		@Override
		public void enterStatic_value(LutEquationParser.Static_valueContext ctx) {
			isVcc = ctx.getText().equals("1");
			isGnd = ctx.getText().equals("0");
		}

		@Override
		public void enterInit_string(LutEquationParser.Init_stringContext ctx) {
			setContents(new LutContents(InitString.parse(ctx.getText())));
		}

		@Override
		public void enterEquation_value(LutEquationParser.Equation_valueContext ctx) {
			LutEquation eqn = new EqnParserVisitor().visitEquation(ctx.equation());
			setContents(new LutContents(eqn));
		}
	}

	@Override
	public String toString() {
		return "#" + operatingMode + ":" + getOutputPinName() + "=" +
				(isVccSource() ? "1" : (isGndSource() ? "0" : contents.toString()));
	}
}

package org.DitaSemia.Ot.AdvancedKeyRef;

import org.DitaSemia.Base.AdvancedKeyref.KeyDefInterface;
import org.DitaSemia.Ot.DitaSemiaOtResolver;
import org.apache.log4j.Logger;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;

public class GetIsFilteredKeyCall extends ExtensionFunctionCall {
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(GetIsFilteredKeyCall.class.getName());

	protected final DitaSemiaOtResolver otResolver;

	public GetIsFilteredKeyCall(DitaSemiaOtResolver otResolver) {
		this.otResolver	= otResolver;
	}

	@Override
	public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
		final Item keyDefItem = arguments[0].head();
		if (keyDefItem != null) {
			final KeyDefInterface keyDef = DitaSemiaOtResolver.getKeyDefFromItem(keyDefItem);
			//logger.info(keyDef + ": " + keyDef.isFilteredKey());
			return (keyDef.isFilteredKey()) ? BooleanValue.TRUE : BooleanValue.FALSE;
		} else {
			return BooleanValue.FALSE;
		}
	}

}

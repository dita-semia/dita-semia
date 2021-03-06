package org.DitaSemia.Base.ExtensionFunctions;

import org.DitaSemia.Base.DitaUtil;
import org.DitaSemia.Base.AdvancedKeyref.KeyDef;
import org.DitaSemia.Base.DocumentCaching.BookCacheProvider;

import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.value.SequenceType;

public class GetAncestorPathDef extends ExtensionFunctionDefinition {
	
	public static final String LOCAL_NAME	= "getAncestorPath"; 

	protected final BookCacheProvider 	bookCacheProvider;
	
	public GetAncestorPathDef(BookCacheProvider bookCacheProvider) {
		this.bookCacheProvider	= bookCacheProvider;
	}
	
	@Override
		public SequenceType[] getArgumentTypes() {
			SequenceType[] sequenceType = {SequenceType.SINGLE_ELEMENT_NODE, SequenceType.SINGLE_STRING};
			return sequenceType;
	}

	@Override
	public StructuredQName getFunctionQName() {
		return new StructuredQName(DitaUtil.NAMESPACE_PREFIX, DitaUtil.NAMESPACE_URI, LOCAL_NAME);
	}

	@Override
	public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
		return SequenceType.STRING_SEQUENCE;
	}

	@Override
	public ExtensionFunctionCall makeCallExpression() {
		return new GetAncestorPathCall(bookCacheProvider);
	}

}

/*
 * This file is part of the DITA-SEMIA project (www.dita-semia.org).
 * See the accompanying LICENSE file for applicable licenses.
 */

package org.DitaSemia.Oxygen.ExtensionBundle;

import java.net.URL;

import org.DitaSemia.Base.DocumentCache;
import org.DitaSemia.Base.DocumentCacheInitializer;
import org.DitaSemia.Base.XsltConref.XsltConref;
import org.DitaSemia.Oxygen.AuthorNodeWrapper;
import org.DitaSemia.Oxygen.DocumentCacheHandler;
import org.apache.log4j.Logger;

import ro.sync.ecss.extensions.api.AuthorReferenceResolver;
import ro.sync.ecss.extensions.api.StylesFilter;
import ro.sync.ecss.extensions.api.link.LinkTextResolver;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.ecss.extensions.dita.DITAExtensionsBundle;

public class DitaSemiaExtensionBundle extends DITAExtensionsBundle {

	private static final String KEY_TYPE_DEF_LIST_URL			= "KeyTypeDefList.xml";
	
	@SuppressWarnings("unused")
	private static final Logger logger = Logger.getLogger(DitaSemiaExtensionBundle.class.getName());
	
	
	public static void initDocumentCacheHandler() {
		final DocumentCacheHandler cacheHandler = DocumentCacheHandler.getInstance();
		if (cacheHandler.getInitializer() == null) {
			cacheHandler.setInitializer(new DocumentCacheInitializer() {
				@Override
				public void initDocumentCache(DocumentCache documentCache) {
					final URL keyTypeDefListUrl = getClass().getClassLoader().getResource(KEY_TYPE_DEF_LIST_URL);
					if (keyTypeDefListUrl != null) {
						documentCache.parseKeyTypeDefFile(keyTypeDefListUrl);
					}
				}});
		}
	}
	
	public DitaSemiaExtensionBundle() {
		initDocumentCacheHandler();
	}
	
	@Override
	public String getDescription() {
		return "DITA-SEMIA extension bundle";
	}
	
	
	@Override
	public boolean isContentReference(AuthorNode node) {
		boolean isContentReference = false;
		if (XsltConref.isXsltConref(new AuthorNodeWrapper(node, null))) {
			isContentReference = true;
		} else {
			isContentReference = super.isContentReference(node);
		}
		return isContentReference;
	}
	
	
	@Override
	public AuthorReferenceResolver createAuthorReferenceResolver() {
		return new DitaSemiaReferenceResolver();
	}
	
	@Override
	public LinkTextResolver createLinkTextResolver() {
		return new DitaSemiaLinkTextResolver();
	}
	
	
	@Override
	public StylesFilter createAuthorStylesFilter() {
		return new DitaSemiaStylesFilter();
	}

}
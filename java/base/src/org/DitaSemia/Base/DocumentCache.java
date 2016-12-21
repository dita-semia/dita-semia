package org.DitaSemia.Base;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;

import org.DitaSemia.Base.AdvancedKeyref.KeyDef;
import org.DitaSemia.Base.AdvancedKeyref.KeyDefInterface;
import org.DitaSemia.Base.AdvancedKeyref.KeyDefListInterface;
import org.DitaSemia.Base.AdvancedKeyref.KeyTypeDef;
import org.DitaSemia.Base.AdvancedKeyref.KeyTypeDefListInterface;
import org.DitaSemia.Base.AdvancedKeyref.KeyspecInterface;
import org.DitaSemia.Base.AdvancedKeyref.ExtensionFunctions.AncestorPathDef;
import org.DitaSemia.Base.DocumentCaching.CachedFile;
import org.DitaSemia.Base.DocumentCaching.CachedTopicRef;
import org.DitaSemia.Base.XsltConref.XsltConref;
import org.apache.log4j.Logger;

public class DocumentCache extends SaxonConfigurationFactory implements KeyDefListInterface, KeyTypeDefListInterface {

	private static final Logger logger = Logger.getLogger(DocumentCache.class.getName());

	public static final String 	NAMESPACE_URI			= "http://www.dita-semia.org/document-cache";
	public static final String 	NAMESPACE_PREFIX		= "dc";

	public static final String	ATTR_ID					= "id";
	public static final String	ATTR_CLASS				= "class";
	public static final String	ATTR_PROCESSING_ROLE	= "processing-role";
	public static final String	ATTR_HREF				= "href";
	public static final String	ATTR_NAME				= "name";
	public static final String	ATTR_VALUE				= "value";
	public static final String	ATTR_XTRF				= "xtrf";
	
	public static final String	CLASS_TOPIC_REF			= " map/topicref ";
	public static final String	CLASS_DATA				= " topic/data ";
	public static final String	CLASS_MAP				= " map/map ";
	
	public static final String	ROLE_RESOURCE_ONLY		= "resource-only";
	
	public static final String	DATA_NAME_TYPE_DEF_URI	= "ikd:TypeDefUri";
	public static final QName	NAME_KEY_TYPE_DEF_LIST	= new QName("KeyTypeDefList");
	public static final QName	NAME_KEY_TYPE_DEF		= new QName(KeyTypeDef.ELEMENT);
	
	public static final String 	URL_ID_DELIMITER		= "#";
	public static final String 	ANY_KEY_TYPE			= "*";

	public static final String 	CONFIG_FILE_URL 		= "/cfg/document-cache-saxon-config.xml";
	
	private final URL 				rootDocumentUrl;
	private final XPathCache 		xPathCache;
	
	private HashMap<String, KeyDefInterface> 	keyDefByRefString 	= new HashMap<>();
	private HashMap<String, KeyDefInterface> 	keyDefByLocation	= new HashMap<>();
	private HashMap<String, KeyTypeDef> 		keyTypeDefByName	= new HashMap<>();
	private HashMap<String, CachedFile> 		cachedFileByUrl		= new HashMap<>();
	private HashMap<String, CachedTopicRef> 	cachedTopicRefByUrl	= new HashMap<>();
	
	
	private final SaxonConfigurationFactory configurationFactory;
	private final SaxonDocumentBuilder		documentBuilder;
	private final Configuration				defaultConfiguration;
	private final DocumentCacheInitializer	initializer;

		
	/* used by oXygen */
	public DocumentCache(URL rootDocumentUrl, DocumentCacheInitializer initializer, SaxonConfigurationFactory configurationFactory) {
		this.rootDocumentUrl 		= rootDocumentUrl;
		this.initializer			= initializer;
		this.configurationFactory	= configurationFactory;
		this.defaultConfiguration	= this.configurationFactory.createConfiguration();
		this.documentBuilder		= new SaxonCachedDocumentBuilder(this);

		registerExtensionFunctions(defaultConfiguration);
		xPathCache			= createXPathCache(defaultConfiguration);
	}

	/* used by OT */
	public DocumentCache(URL rootDocumentUrl, DocumentCacheInitializer initializer, Configuration baseConfiguration) {
		registerExtensionFunctions(baseConfiguration);

		this.rootDocumentUrl 		= rootDocumentUrl;
		this.initializer			= initializer;
		this.configurationFactory	= null;
		this.defaultConfiguration	= baseConfiguration;
		this.documentBuilder		= new SaxonDocumentBuilder(defaultConfiguration);

		xPathCache			= createXPathCache(defaultConfiguration);
	}


	public void fillCache() {
		try {
			final long startTime = Calendar.getInstance().getTimeInMillis();
			
			if (initializer != null) {
				initializer.initDocumentCache(this);
			}
			
			final Source 		source 	= defaultConfiguration.getURIResolver().resolve(rootDocumentUrl.toString(), "");
			final CachedFile	file	= createFile(source);
			if (file != null) {
				parseFile(file);
				final long time = Calendar.getInstance().getTimeInMillis() - startTime;
				logger.info("fillCache done: " + time + "ms, " + cachedFileByUrl.size() + " files, " + keyDefByRefString.size() + " keys (" + decodeUrl(source.getSystemId()) + ")");
			}
		} catch (TransformerException e) {
			logger.error(e);
		}
	}
	
	@Override
	public Configuration createConfiguration() {
		final Configuration configuration = configurationFactory.createConfiguration();
		registerExtensionFunctions(configuration);
		return configuration;
	}
	
	public static XPathCache createXPathCache(Configuration configuration) {
		XPathCache xPathCache = new XPathCache(configuration);
		xPathCache.declareNamespace(KeyDef.NAMESPACE_PREFIX, KeyDef.NAMESPACE_URI);
		return xPathCache;
	}
	
	public URL getRootDocumentUrl() {
		return rootDocumentUrl;
	}
	
	public boolean isUrlIncluded(URL url) {
		final String urlDecoded = decodeUrl(url);
		if (urlDecoded == null) {
			//logger.info("isUrlIncluded(" + url + ") -> null");
			return false;
		} else {
			//logger.info("isUrlIncluded(" + url + ") -> '" + urlDecoded + "', " + decodedUrlList.contains(urlDecoded));
			return cachedFileByUrl.containsKey(urlDecoded);
		}
	}

	public static String decodeUrl(URL url) {
		return decodeUrl(url.toString());
	}

	public static String decodeUrl(String url) {
		try {
			return URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	public static Configuration createBaseConfiguration() {
		return SaxonConfigurationFactory.loadConfiguration(XsltConref.class.getResource(CONFIG_FILE_URL));
	}
	
	protected void registerExtensionFunctions(Configuration configuration) {
		configuration.registerExtensionFunction(new AncestorPathDef(this));
	}

	private CachedFile createFile(Source source) {
		final String 	decodedUrl	= decodeUrl(source.getSystemId());
		CachedFile 		cachedFile	= null;

		if (cachedFileByUrl.containsKey(decodedUrl)) {
			logger.error("ERROR: File included twice: '" + decodedUrl + "' - ignored second one.");
		} else {
			try {
				// remove the provided xml reader to force saxon creating its own one using the configuration and, thus, expanding the attribute defaults
				if (source instanceof SAXSource) {
					((SAXSource)source).setXMLReader(null);
				}

				final XdmNode 				rootNode 		= documentBuilder.build(source);
				final XdmSequenceIterator 	iterator 		= rootNode.axisIterator(Axis.CHILD);
				
				//logger.info(SaxonNodeWrapper.serializeNode(rootNode.getUnderlyingNode()));
				
				XdmNode rootElement = null;
				while ((iterator.hasNext()) && (rootElement == null)) {
					final XdmNode node =(XdmNode)iterator.next();
					//logger.info("  node: " + node.getNodeName() + ", " + node.getNodeKind());
					if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
						rootElement = node;
					}
				}
				if (rootElement == null) {
					throw new Exception("No root element.");
				}
				
				//logger.info("rootElement: " + rootElement.getNodeName());
				
				cachedFile = new CachedFile(decodedUrl, rootElement, new SaxonNodeWrapper(rootElement.getUnderlyingNode(), xPathCache));
				cachedFileByUrl.put(decodedUrl, cachedFile);	// first insert file into map before parsing it to avoid recursions when the cache is tried to be accessed during parsing.

				//logger.info("created file: '" + decodedUrl + "'");
				
			} catch (Exception e) {
				logger.error("Error parsing file '" + decodedUrl +"':");
				logger.error(e, e);
				cachedFile = null;
			}
		}
		return cachedFile;
	}
	
	private void parseFile(CachedFile cachedFile) throws TransformerException {
		//logger.info("parseFile: " + cachedFile);
		parseNode(cachedFile.getRootNode(), cachedFile, null);
	}

	private void parseNode(XdmNode node, CachedFile cachedFile, CachedTopicRef parentTopicRef) throws TransformerException {
		//logger.info("parseNode: " + node.getUnderlyingNode().getDisplayName() + ", " + node.getNodeKind());
		if (node.getNodeKind() == XdmNodeKind.ELEMENT) {
			
			final SaxonNodeWrapper nodeWrapper = new SaxonNodeWrapper(node.getUnderlyingNode(), xPathCache);
			
			final KeyDef keyDef = KeyDef.fromNode(nodeWrapper);
			if (keyDef != null) {
				addKeyDef(keyDef);
			}
			
			final String classAttr = nodeWrapper.getAttribute(ATTR_CLASS, null);

			//logger.info("parsing element: <" + nodeWrapper.getName() + " class=\"" + classAttr + "\">");

			if (classAttr != null) {
				final CachedTopicRef topicRef = parseTopicRef(nodeWrapper, classAttr, cachedFile, parentTopicRef);
				if (topicRef != null) {
					parentTopicRef = topicRef;	// take it as new parent for child nodes.
				}
				parseKeyTypeDefNode(nodeWrapper, classAttr);
			}
			
			final XdmSequenceIterator iterator = node.axisIterator(Axis.CHILD);
			while (iterator.hasNext()) {
				parseNode((XdmNode)iterator.next(), cachedFile, parentTopicRef);
			}
		}
	}
	
	private CachedTopicRef parseTopicRef(NodeWrapper nodeWrapper, String classAttr, CachedFile containingFile, CachedTopicRef parentTopicRef) throws TransformerException {
		//logger.info("parseTopicRef");
		CachedTopicRef topicRef = null;
		if (classAttr.contains(CLASS_TOPIC_REF)) {
			final String processingRoleAttr = nodeWrapper.getAttribute(ATTR_PROCESSING_ROLE, null);
			if ((processingRoleAttr == null) || (!processingRoleAttr.equals(ROLE_RESOURCE_ONLY))) {
				final String href = nodeWrapper.getAttribute(ATTR_HREF, null);
				if ((href != null) && (!href.isEmpty())) {
					final CachedFile refFile = createFile(nodeWrapper.resolveUri(href));
					if (refFile != null) {
						topicRef = new CachedTopicRef(containingFile, refFile, parentTopicRef);
						cachedTopicRefByUrl.put(refFile.getDecodedUrl(), topicRef);
						//logger.info("new topicRef: " + topicRef + ", " + refFile.getDecodedUrl());
						parseFile(refFile);
					}
				}
			}
		}
		return topicRef;
	}


	private void parseKeyTypeDefNode(NodeWrapper nodeWrapper, String classAttr) throws TransformerException {
		if (classAttr.contains(CLASS_DATA)) {
			final String nameAttr = nodeWrapper.getAttribute(ATTR_NAME, null);
			if ((nameAttr != null) && (nameAttr.equals(DATA_NAME_TYPE_DEF_URI))) {
				final String urlAttr = nodeWrapper.getAttribute(ATTR_VALUE, null);
				if ((urlAttr != null) && (!urlAttr.isEmpty())) {
					Source source;
					final String xtrfAttr = nodeWrapper.getAttribute(ATTR_XTRF, null);
					if ((xtrfAttr != null) && (!xtrfAttr.isEmpty())) {
						// resolve URI when being called from DitaSemiaOtResolver
						source = nodeWrapper.getUriResolver().resolve(urlAttr, xtrfAttr);
					} else {
						source = nodeWrapper.resolveUri(urlAttr);
					}
					parseKeyTypeDefSource(source);
				}
			}
		}
	}


	public void parseKeyTypeDefFile(URL url) {
		try {
			final InputStream inputStream = new BufferedInputStream(new FileInputStream(url.getFile()));
			final StreamSource 	source 		= new StreamSource(inputStream);
			source.setSystemId(url.toExternalForm());
			//logger.info("source.getSystemId(): " + source.getSystemId());
			parseKeyTypeDefSource(source);
		} catch (FileNotFoundException e) {
			logger.error(e, e);
		}
	}


	public void parseKeyTypeDefSource(Source source) {
		try {
			final XdmNode 				rootNode = documentBuilder.build(source);
			final XdmSequenceIterator 	iterator = rootNode.axisIterator(Axis.CHILD, NAME_KEY_TYPE_DEF_LIST);
			
			while (iterator.hasNext()) {
				createKeyTypeDef((XdmNode)iterator.next(), KeyTypeDef.DEFAULT);
			}
		} catch (Exception e) {
			logger.error("Error parsing KeyTypeDef file '" + decodeUrl(source.getSystemId()) + "':");
			logger.error(e, e);
		}
	}
	

	public void refresh() {
		//logger.info("refresh");

		keyDefByRefString.clear();
		keyDefByLocation.clear();
		keyTypeDefByName.clear();
		cachedFileByUrl.clear();
		cachedTopicRefByUrl.clear();
		
		if (documentBuilder instanceof SaxonCachedDocumentBuilder) {
			((SaxonCachedDocumentBuilder)documentBuilder).clearCache();
		}

		fillCache();
	}

	private void addKeyDef(KeyDefInterface keyDef) {
		keyDefByRefString.put(keyDef.getRefString(), keyDef);
		keyDefByLocation.put(keyDef.getDefLocation(), keyDef);
	}

	@Override
	public Collection<KeyDefInterface> getKeyDefs() {
		return keyDefByRefString.values();
	}

	@Override
	public KeyDefInterface getExactMatch(KeyspecInterface keyspec) {
		return getExactMatch(keyspec.getRefString());
	}

	@Override
	public KeyDefInterface getExactMatch(String refString) {
		return keyDefByRefString.get(refString);
	}

	@Override
	public XPathCache getXPathCache() {
		return xPathCache;
	}

	@Override
	public KeyDefInterface getAncestorKeyDef(NodeWrapper node, String keyType) {
		//logger.info("getAncestorKeyDef (" + ((node.isSameNode(node.getRootNode())) ? "#document" : node.getName()) + ", " + keyType + ")");
		final NodeWrapper parent = getParent(node);
		if (parent != null) {
			final String		id		= parent.getAttribute(ATTR_ID, null);
			KeyDefInterface 	keyDef 	= null; 
			if ((id != null) && (!id.isEmpty())) {
				final String location = getNodeLocation(parent.getBaseUrl(), id);
				keyDef = keyDefByLocation.get(location);
				//logger.info("  location: '" + location + "', keyDef: " + keyDef);
				if ((keyDef != null) && 
						(keyType != null) && 
						(!keyType.equals(ANY_KEY_TYPE)) && 
						(!keyType.equals(keyDef.getType()))){
					keyDef = null;	// type is not matching
				}
			}
			if (keyDef != null) {
				//logger.info("  keyDef: " + keyDef);
				return keyDef;
			} else {
				return getAncestorKeyDef(parent, keyType);
			}
		} else {
			//logger.info("  keyDef: " + null);
			return null;
		}
	}
	
	public Collection<CachedFile> getChildTopics(NodeWrapper topicNode) {
		//logger.info("getChildTopics(" + topicNode.getName() + ")");
		final String 			decodedUrl 	= decodeUrl(topicNode.getBaseUrl());
		final CachedTopicRef	topicRef	= cachedTopicRefByUrl.get(decodedUrl);
		//logger.info("topicRef: " + topicRef);
		if (topicRef != null) {
			return topicRef.getChildTopics();
		} else {
			return null;
		}
	}

	private NodeWrapper getParent(NodeWrapper node) {
		NodeWrapper parent = node.getParent();
		if (parent == null) {
			final String 			decodedUrl 		= decodeUrl(node.getBaseUrl());
			final CachedTopicRef	parentTopicRef	= getParentTopicRef(decodedUrl);
			if (parentTopicRef != null) {
				//logger.info("parent: " + parentTopicRef.getReferencedFile().getRootWrapper().getName());
				return parentTopicRef.getReferencedFile().getRootWrapper();
			} else {
				return null;
			} 
		} else {
			return parent;
		}
	}

	private CachedTopicRef getParentTopicRef(String decodedUrl) {
		//logger.info("getParentTopicRef " + decodedUrl);
		final CachedTopicRef topicRef = cachedTopicRefByUrl.get(decodedUrl);
		if (topicRef != null) {
			final CachedTopicRef parentTopicRef = topicRef.getParentTopicRef();
			if (parentTopicRef != null) {
				//logger.info("  -> " + parentTopicRef.getReferencedFile().getDecodedUrl());
				return parentTopicRef;
			} else {
				return getParentTopicRef(topicRef.getContainingFile().getDecodedUrl());
			}
		} else {
			//logger.info("  null");
			return null;
		}
	}

	private String getNodeLocation(URL url, String id) {
		return decodeUrl(url) + URL_ID_DELIMITER + id;
	}
	
	private void createKeyTypeDef(XdmNode node, KeyTypeDef parent) {
		KeyTypeDef childParent = parent;
		//logger.info("nodeName: " + node.getNodeName());
		if (node.getNodeName().equals(NAME_KEY_TYPE_DEF)) {
			final KeyTypeDef keyTypeDef = KeyTypeDef.fromNode(new SaxonNodeWrapper(node.getUnderlyingNode(), null), parent);
			keyTypeDefByName.put(keyTypeDef.getName(), keyTypeDef);
			childParent = keyTypeDef;
			//logger.info("keyTypeDef: " + keyTypeDef);
		}
		final XdmSequenceIterator 	iterator = node.axisIterator(Axis.CHILD, NAME_KEY_TYPE_DEF);
		while (iterator.hasNext()) {
			createKeyTypeDef((XdmNode)iterator.next(), childParent);
		}
	}

	@Override
	public KeyTypeDef getKeyTypeDef(String typeName) {
		final KeyTypeDef keyTypeDef = keyTypeDefByName.get(typeName);
		return (keyTypeDef != null) ? keyTypeDef : KeyTypeDef.DEFAULT;
	}

}
package org.xmlcml.graphics.svg.fonts;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.util.log.Log;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Transform2;
import org.xmlcml.euclid.util.MultisetUtil;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGPath;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.graphics.svg.cache.ComponentCache;
import org.xmlcml.graphics.svg.path.MovePrimitive;
import org.xmlcml.graphics.svg.path.PathPrimitiveList;
import org.xmlcml.graphics.svg.plot.XPlotBox;
import org.xmlcml.xml.XMLUtil;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

import nu.xom.Attribute;

/** holds the glyphs for a font
 * 
 * @author pm286
 *
 */
public class GlyphSet {
	private static final String GLYPH_LIST = "glyphList";
	private static final String GLYPH_SET = "glyphSet";
	private static String CHARACTER = "character";
	
	private static final Logger LOG = Logger.getLogger(GlyphSet.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private Multiset<String> signatureSet;
	private Multimap<String, SVGGlyph> glyphMapBySignature;
	private HashMap<String, String> characterBySignatureMap;
	private List<Multiset.Entry<String>> signatureListSortedByCount;
	public GlyphSet() {
		
	}
	
	public void addToSetsAndMaps(SVGGlyph glyph) {
		getOrCreateSignatureSet();
		signatureSet.add(glyph.getOrCreateSignatureAttributeValue());
		getOrCreateGlyphBySignatureMap();
		glyphMapBySignature.put(glyph.getOrCreateSignature(), glyph);
	}

	public Multiset<String> getOrCreateSignatureSet() {
		if (signatureSet == null) {
			signatureSet = HashMultiset.create();
		}
		return signatureSet;
	}

	public Multimap<String, SVGGlyph> getOrCreateGlyphBySignatureMap() {
		if (glyphMapBySignature == null) {
			glyphMapBySignature = ArrayListMultimap.create();
		}
		return glyphMapBySignature;
	}

	public HashMap<String, String> getOrCreateCharacterMapBySignature() {
		if (characterBySignatureMap == null) {
			characterBySignatureMap = new HashMap<String, String>();
		}
		return characterBySignatureMap;
	}

	public List<Multiset.Entry<String>> getOrCreateSignaturesSortedByCount() {
		signatureSet = getOrCreateSignatureSet();
		signatureListSortedByCount = MultisetUtil.createStringListSortedByCount(signatureSet);
		return signatureListSortedByCount;
	}

	public List<Multiset.Entry<String>> getGlyphsSortedBySignatureCount() {
		Multiset<String> signatureSet = getOrCreateSignatureSet();
		List<Multiset.Entry<String>> sigsByCount = MultisetUtil.createStringListSortedByCount(signatureSet);
		return sigsByCount;
	}

	public void createGlyphSetsAndAnalyze(String fileroot, File outputDir, File inputFile) {
		SVGElement svgElement = SVGElement.readAndCreateSVG(inputFile);
		XPlotBox xPlotBox = new XPlotBox();
		ComponentCache componentCache = new ComponentCache(xPlotBox); 
		componentCache.readGraphicsComponentsAndMakeCaches(svgElement);
		List<SVGPath> paths = componentCache.getOrCreatePathCache().getCurrentPathList();
		addPathsToSetsAndMaps(paths);
		writeGlyphsForEachSig(fileroot, outputDir);
	}

	private void writeGlyphsForEachSig(String fileroot, File outputDir) {
		signatureListSortedByCount = getOrCreateSignaturesSortedByCount();
		for (int i = 0; i < signatureListSortedByCount.size(); i++) {
			Entry<String> sigEntry = signatureListSortedByCount.get(i);
			String sig = sigEntry.getElement();
			List<SVGGlyph> glyphList = new ArrayList<SVGGlyph>(getOrCreateGlyphBySignatureMap().get(sig));
			SVGG g = this.createSVG(glyphList, i);
			SVGSVG.wrapAndWriteAsSVG(g, new File(outputDir, fileroot+"/"+"glyph."+i+".svg"), 300, 100);
		}
	}

	private void addPathsToSetsAndMaps(List<SVGPath> paths) {
		for (SVGPath path : paths) {
			PathPrimitiveList pathPrimitiveList = path.getOrCreatePathPrimitiveList();
			List<PathPrimitiveList> pathPrimitiveListList = pathPrimitiveList.splitBefore(MovePrimitive.class);
			for (PathPrimitiveList primitiveList : pathPrimitiveListList) {
				SVGGlyph outlineGlyph = SVGGlyph.createRelativeToBBoxOrigin(primitiveList);
				addToSetsAndMaps(outlineGlyph);
			}
		}
	}

	public SVGG createSVG(List<SVGGlyph> glyphList, int serial) {
		SVGG g = new SVGG();
		Transform2 t2 = Transform2.applyScale(5.0);
		g.appendChild(SVGText.createDefaultText(new Real2(10,10.), ""+serial+" "+glyphList.get(0).getOrCreateSignature()));
		for (SVGGlyph glyph : glyphList) {
			// have to add copy() or add to SVGElement
			SVGPath glyph1 = (SVGPath) glyph.copy();
			glyph1.setTransform(t2);
			g.appendChild(glyph1);
		}
		return g;
	}

	public List<SVGGlyph> getGlyphBySig(String sig) {
		Multimap<String, SVGGlyph> glyphMapBySig = getOrCreateGlyphBySignatureMap();
		return glyphMapBySig == null ? null : new ArrayList<SVGGlyph>(glyphMapBySignature.get(sig));
	}

	/** writes glyset as XML.
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void writeGlyphSet(File file) throws IOException {
		SVGElement glyphSetXml = new SVGElement(GLYPH_SET);
		for (String signature : glyphMapBySignature.keySet()) {
			SVGElement sigXml = new SVGElement(GLYPH_LIST);
			sigXml.addAttribute(new Attribute(SVGPath.SIGNATURE, signature));
			glyphSetXml.appendChild(sigXml);
			List<SVGGlyph> glyphs = new ArrayList<SVGGlyph>(glyphMapBySignature.get(signature));
			for (SVGGlyph glyph : glyphs) {
				sigXml.appendChild(glyph.copy());
			}
		}
		XMLUtil.debug(glyphSetXml, new FileOutputStream(file), 1);
	}

	public static GlyphSet readGlyphSet(File file) {
		GlyphSet glyphSet = new GlyphSet();
		SVGElement glyphSetXml = SVGElement.readAndCreateSVG(file);
//		List<SVGElement> glyphList = SVGUtil.getQuerySVGElements(glyphSetXml, "./*/*[local-name()='"+GLYPH_LIST+"']");
		// no local name yet
		List<SVGElement> glyphList = SVGUtil.getQuerySVGElements(glyphSetXml, "./*");
		for (SVGElement glyph : glyphList) {
			String signature = glyph.getAttributeValue(SVGPath.SIGNATURE);
			glyphSet.getOrCreateSignatureSet().add(signature);
			String character = glyph.getAttributeValue(GlyphSet.CHARACTER);
			glyphSet.getOrCreateCharacterMapBySignature().put(signature, character);
		}
		return glyphSet;
	}

	
	public String getCharacterBySignature(String signature) {
		getOrCreateCharacterMapBySignature();
		return characterBySignatureMap.get(signature);
	}
	
	
}

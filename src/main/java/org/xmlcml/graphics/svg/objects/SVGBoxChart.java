package org.xmlcml.graphics.svg.objects;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.GraphicsElement;

public class SVGBoxChart extends SVGDiagram {
	
	private static final Logger LOG = Logger.getLogger(SVGBoxChart.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	public static final String BOX_CHART = "boxChart";

	public SVGBoxChart() {
		super();
		this.setClassName(BOX_CHART);
	}
	
	public SVGBoxChart(GraphicsElement diagram) {
		this();
		this.rawDiagram = diagram;
	}

	public void createChart() {
		createPathsTextAndShapes();
		this.createTextBoxes();
		this.createArrows(eps);
		this.findConnectors(eps);
		for (SVGConnector link : connectorList) {
//			System.err.println("=================== \n"+link.toString()+"\n===================");
		}
	}
	
}
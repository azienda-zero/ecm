package it.tredi.ecm.cogeaps;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipInputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;
import org.dom4j.Element;
import org.dom4j.DocumentHelper;
import org.dom4j.Document;

import it.tredi.ecm.dao.entity.AnagrafeRegionaleCrediti;
import it.tredi.ecm.dao.entity.Evento;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;


public class XmlReportValidator {

	public static void validateXmlWithXsd(String fileName, byte []reportEventoXml, Schema schema) throws Exception {
		//estrazione xml
		reportEventoXml = extractXml(fileName, reportEventoXml);

	    Validator validator = schema.newValidator();
	    Source source = new StreamSource(new StringReader(new String(reportEventoXml, Helper.XML_REPORT_ENCODING)));
		validator.validate(source);
	}

	public static void validateEventoXmlWithDb(String fileName, byte []reportEventoXml, Evento evento) throws Exception {
		validateEventoXmlWithDb(fileName, reportEventoXml, Helper.createEventoDataMapFromEvento(evento));
	}

	private static void validateEventoXmlWithDb(String fileName, byte []reportEventoXml, Map<String, String> dbEventoDataMap) throws Exception {
		//estrazione xml
		reportEventoXml = extractXml(fileName, reportEventoXml);

		//ora siamo sicuri di avere un XML
		Document xmlDoc = DocumentHelper.parseText(new String(reportEventoXml, Helper.XML_REPORT_ENCODING));
		Element eventoEl = xmlDoc.getRootElement().element("evento");
		for (String evento_field:Helper.EVENTO_XML_ATTRIBUTES) {
			String xmlValue = eventoEl.attributeValue(evento_field);
			String dbValue = dbEventoDataMap.get(evento_field);
			if(!evento_field.equalsIgnoreCase("num_part"))
				if (!xmlValue.equals(dbValue))
					throw new Exception("I dati dell'evento non corrispondono a quelli memorizzati nel database: [" + evento_field + "]: '" + xmlValue + "' - '" + dbValue + "'");
		}
	}

	public static byte []extractXml(String fileName, byte []data) throws Exception {
		//se P7M -> sbusto
		if (fileName.trim().toUpperCase().endsWith(".P7M"))
			data = extraxtFromP7m(data);

		//se ZIP -> unzip
		if (fileName.trim().toUpperCase().endsWith(".ZIP.P7M"))
			data = extractFromZip(data);

		return data;
	}

	private static byte []extraxtFromP7m(byte []signed_b) throws Exception {
		CMSSignedData csd = new CMSSignedData(signed_b);
		CMSProcessableByteArray cpb = (CMSProcessableByteArray)csd.getSignedContent();
		return (byte[])cpb.getContent();
	}

    private static byte []extractFromZip(byte []zip_b) throws Exception {
		ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zip_b));
		zin.getNextEntry(); //si suppone che ci sia un solo file dentro lo zip
		//TODO - mettere controlli di errore? controllare se file e controllare estensione dentro?
		int count;
		final int BUFFER = 2048;
        byte data[] = new byte[BUFFER];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((count = zin.read(data, 0, BUFFER)) != -1) {
			bos.write(data, 0, count);
		}
		bos.flush();
	    bos.close();
		return bos.toByteArray();
    }

    public static Set<AnagrafeRegionaleCrediti> extractAnagrafeRegionaleCreditiPartecipantiFromXml(String fileName, byte []reportEventoXml) throws Exception {
		//estrazione xml
    	Set<AnagrafeRegionaleCrediti> items = new HashSet<AnagrafeRegionaleCrediti>();
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Helper.PARTECIPANTE_DATA_FORMAT);

		reportEventoXml = extractXml(fileName, reportEventoXml);

		//ora siamo sicuri di avere un XML
		Document xmlDoc = DocumentHelper.parseText(new String(reportEventoXml, Helper.XML_REPORT_ENCODING));
		Element eventoEl = xmlDoc.getRootElement().element("evento");

		List<Element> partecipanti = eventoEl.elements("partecipante");
		for (Element p : partecipanti) {

			items.add(new AnagrafeRegionaleCrediti(p.attributeValue(Helper.PARTECIPANTE_XML_ATTRIBUTES[0]),//cod_fisc
													p.attributeValue(Helper.PARTECIPANTE_XML_ATTRIBUTES[1]),//cognome
													p.attributeValue(Helper.PARTECIPANTE_XML_ATTRIBUTES[2]),//nome
													p.attributeValue(Helper.PARTECIPANTE_XML_ATTRIBUTES[3]),//ruolo
													new BigDecimal(p.attributeValue(Helper.PARTECIPANTE_XML_ATTRIBUTES[4])),//cred_acq
													LocalDate.parse(p.attributeValue(Helper.PARTECIPANTE_XML_ATTRIBUTES[5]), formatter)));//data_acq
		}

		return items;
	}

}

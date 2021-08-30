package org.grobid.core.main.batch;

public class GrobidMedicalReportMainArgs extends GrobidMainArgs {
	// French is the default language
	public String lang = "fr";

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}
}
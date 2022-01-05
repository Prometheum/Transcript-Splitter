package main;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.TextToPDF;

public class Main {

	static File folder = new File(".\\");
	static File[] listOfFiles = folder.listFiles(f -> f.getName().contains(".txt")
			&& (f.getName().toLowerCase().contains("adm-") || f.getName().toLowerCase().contains("edu-")));
	static int year = Calendar.getInstance().get(Calendar.YEAR) + 1;

	public static void main(String[] args) throws Exception {
		System.out.println(""+(year-1));
		if (!new File(".\\Split").exists())
			new File(".\\Split").mkdir();
		if (!new File(".\\Imported").exists())
			new File(".\\Imported").mkdir();
		
		BufferedReader reader = null;
		BufferedWriter writer = null;
		ArrayList<String> list = new ArrayList<String>();
		String[] ouacNum = new String[2];

		for (File f : listOfFiles) {
			if (f.getName().contains(".txt")) {
				try {
					reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
					String tmp;
					// int lineNum=0;
					String OUAC = "";

					while ((tmp = reader.readLine()) != null) {
						// lineNum++;

						// find OUAC number in the transcript and split the string.
						if (tmp.contains("OUAC: ")) {
							ouacNum = new String[2];
							ouacNum = tmp.split("OUAC: ");
							if (ouacNum != null && ouacNum.length >= 2)

								ouacNum[1].replaceAll(" ", "");
							if (ouacNum[1].contains("" + year))
								ouacNum[1] = "" + year + "_" + ouacNum[1].split("" + year)[1];
							
							//check for files from last year
							if (ouacNum[1].contains("" + (year-1)))
								ouacNum[1] = "" + (year-1) + "_" + ouacNum[1].split("" + (year-1))[1];
							
							//check for files from 2 years ago
							if (ouacNum[1].contains("" + (year-2)))
								ouacNum[1] = "" + (year-2) + "_" + ouacNum[1].split("" + (year-2))[1];
							
							//check for files from 3 years ago
							if (ouacNum[1].contains("" + (year-3)))
								ouacNum[1] = "" + (year-3) + "_" + ouacNum[1].split("" + (year-3))[1];
							
							OUAC = removeLastChar(ouacNum[1]);
							OUAC = OUAC + "_";
						}

						String toAdd = tmp;
						// Reformat transcripts to remove control characters, reformat tabs, remove
						// additional page breaks etc.
						toAdd = toAdd.replaceAll("[(]s12H", " ");
						toAdd = toAdd.replaceAll(
								"------------------------------  Official EDI Transcript  ------------------------------",
								"\r\n -------------------------  Official EDI Transcript  -------------------------");
						toAdd = toAdd.replaceAll("\f\f", "\f");
						toAdd = toAdd.replaceAll("\t", "   ");
						toAdd = toAdd.replaceAll("\u001B", "");
						toAdd = toAdd.replaceAll("\u0009", " ");

						// Skip over lines that contain information we don't want - Only print courses
						// and grades
						if (!toAdd.contains("Another Version of Final Mark") && !toAdd.contains("CRS Note:")
								&& !toAdd.contains("Cred Type")) {
							list.add(toAdd);
						}
						int duplicateNum = 0;
						if (tmp.contains("E N D   O F   R E C O R D")) {
							System.out.println("End of File - Creating New File");
							
							while (((new File(".\\" + OUAC + duplicateNum + ".txt").exists()
									|| new File(".\\Converted" +File.separator+ OUAC + duplicateNum + ".txt").exists()) || (new File(".\\" + OUAC + duplicateNum + ".pdf").exists()
									|| new File(".\\Converted" +File.separator+ OUAC + duplicateNum + ".pdf").exists()))) {
								System.out.println("here");
								duplicateNum++;
								Thread.sleep(300);
							}
				

							writer = new BufferedWriter(new FileWriter(".\\" + OUAC + duplicateNum + ".txt"));

							for (int i = 0; i < list.size(); i++) {

								writer.write(list.get(i) + "\r\n");

							}
							list = new ArrayList<String>();
							writer.close();

						}
					}
					reader.close();

				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					reader.close();

				}
				f.renameTo(new File(".\\Split\\" + f.getName()));
				Thread.sleep(300);
				PDFCreator();
				Thread.sleep(300);
			}
		}
	}

	private static String removeLastChar(String str) {
		return str.substring(0, str.length() - 1);
	}

	private static void PDFCreator() throws Exception {
		File folder = new File(".\\");
		File[] listOfFiles = folder.listFiles(f -> f.getName().contains(".txt")
				&& (!f.getName().toLowerCase().contains("adm-") && !f.getName().toLowerCase().contains("edu-")));
		PDDocument doc = null;
		for (File f : listOfFiles) {

			System.out.println(f.getName());

			TextToPDF ttp = new TextToPDF();

			BufferedReader reader = null;
			try {

				File pdfName = new File(".\\" + removeFileExt(f.getName()) + ".pdf");
				reader = new BufferedReader(new FileReader(f.getAbsolutePath()));
				ttp.setFont(PDType1Font.COURIER);
				doc = ttp.createPDFFromText(reader);

				for (int i = 0; i < doc.getNumberOfPages(); i++) {

					// find and delete blank pages
					if (isBlank(doc, i)) {
						// System.out.println("Page " + i + " is blank... deleting page" + i);
						doc.removePage(i);
						doc.save(pdfName);
					}
				}
				doc.save(pdfName);
				doc.close();
				reader.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				// doc.close();
				reader.close();
			} finally {
				doc.close();
			}
			if (!new File(".\\Converted").exists())
				new File(".\\Converted").mkdir();
			f.renameTo(new File(".\\Converted\\" + f.getName()));
			Thread.sleep(300);

		}

	}

	private static Boolean isBlank(PDDocument doc, int pdfPage) throws IOException {
		PDFRenderer pdfRenderer = new PDFRenderer(doc);

		BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(pdfPage, 300, ImageType.RGB);
		long count = 0;
		int height = bufferedImage.getHeight();
		int width = bufferedImage.getWidth();
		Double areaFactor = (width * height) * 1.0;

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				Color c = new Color(bufferedImage.getRGB(x, y));
				// verify light gray and white
				if (c.getRed() == c.getGreen() && c.getRed() == c.getBlue() && c.getRed() >= 248) {
					count++;
				}
			}
		}

		if (count >= areaFactor) {
			return true;
		}

		return false;
	}

	private static String removeFileExt(String str) {
		return str.substring(0, str.length() - 4);
	}
}

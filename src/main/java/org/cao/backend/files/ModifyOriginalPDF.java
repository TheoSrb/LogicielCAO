package org.cao.backend.files;

public class ModifyOriginalPDF {

    public static void main(String[] args) {
        PDFReader pdfReader = new PDFReader("C:\\Users\\theosarbachfischer\\Desktop\\af0pro01095_00.pdf");

        pdfReader.insertClickableZoneTo(0, 200, 500, 200, 30, "https://www.google.com");
    }
}
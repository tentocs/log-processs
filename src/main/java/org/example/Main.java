package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.process.database.SQLDatabaseConnection;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;


public class Main {
    public static void main(String[] args) {

        String xPathExpression = "//Ticket/*"; // Expresión XPath para seleccionar todos los nodos dentro de <Ticket>
        Document documento = null;
        NodeList nodos = null;

        String separator = "";

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {

            SQLDatabaseConnection.createConnection();

            System.out.print("Ingresa la ruta de lectura de xml: ");
            String fromPath = reader.readLine();

            System.out.print("Ingresa la ruta de salida: ");
            String toPath = reader.readLine();

            File directory = new File(fromPath);
            File[] archivos = directory.listFiles();

            if (archivos != null) {
                for (File archivo : archivos) {
                    if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".xml")) {

                        // Carga del documento XML
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        separator = System.getProperty("file.separator");
                        // documento = builder.parse(new File("C:"+separator+"1918.xml"));
                        documento = builder.parse(new File(fromPath + separator + archivo.getName())); // Ajusto la ruta al archivo XML según ubicación


                        // Preparación de XPath
                        XPath xpath = XPathFactory.newInstance().newXPath();

                        // Consulta
                        nodos = (NodeList) xpath.evaluate(xPathExpression, documento, XPathConstants.NODESET);


                        StringBuilder content = new StringBuilder();
                        String ticketNumber = "";
                        String date = "";
                        String hour = "";
                        String codigoPLU = "";
                        String precio = "";
                        String descuento = "";
                        String lastCodigoPLU = "";
                        String rut = "";
                        String cashierName = "";
                        String cashierId = "";
                        String storeNumber ="";
                        String posNumber = "";
                        String numTender = "";
                        String tenderAmount = "";
                        List<String> pluCodes = new ArrayList<>();
                        List<String> duplicates = new ArrayList<>();
                        int totalPrice = 0;
                        int countDuplicates = 0;
                        int totalDiscount = 0;
                        int countDiscount = 0;
                        int finalPrice = 0;
                        for (int i = 0; i < nodos.getLength(); i++) {
                            Node nodo = nodos.item(i);
                            if (nodo.getNodeType() == Node.ELEMENT_NODE) {

                                //Obtengo el Ticket,Fecha y Hora
                                if (nodo.getNodeName().equals("Frame")) {
                                    NamedNodeMap atributos = nodo.getAttributes();
                                    ticketNumber = getAttribute(atributos, "TicketNumber");
                                    //Campo Fecha ajustado
                                    date = convertDateWhitOutBars(getAttribute(atributos, "Tail_Fecha"));
                                    hour = getAttribute(atributos, "Tail_Hora");
                                    cashierId = getAttribute(atributos, "Tail_NumCajero");
                                    storeNumber = getAttribute(atributos, "StoreNumber");
                                    posNumber = getAttribute(atributos, "Tail_NumPOS");

                                }

                                if (nodo.getNodeName().equals("InfoDocData")) {
                                    NamedNodeMap rutAttributes = nodo.getAttributes();
                                    rut = getAttribute(rutAttributes, "DocNumber");
                                }

                                if (nodo.getNodeName().equals("InfoEmployeeID")) {
                                    NamedNodeMap attributes = nodo.getAttributes();
                                    cashierName = getAttribute(attributes, "CashierName");
                                }


                                if (i == 1) {

                                    content = new StringBuilder(getStaticLine(nodos) + " CLOSED          TRUE" + "\n" +
                                            getStaticLine(nodos) + " DOCUMENT_TYPE   TICKET" + "\n" +
                                            getStaticLine(nodos) + " TRX_NUMBER      " + ticketNumber + "\n" +
                                            getStaticLine(nodos) + " EMPLOYEE        CARLOS,CONTENTO, ," + "\n" +
                                            getStaticLine(nodos) + " DOB             " + date + "\n" +
                                            getStaticLine(nodos) + " DATE            " + date + "\n" +
                                            getStaticLine(nodos) + " TIME            " + hour + "\n" +
                                            getStaticLine(nodos) + " HEADER          DEFAULT HEADER" + "\n" +
                                            getStaticLine(nodos) + " FOOTER          DEFAULT FOOTER" + "\n");

                                    pluCodes = resolveString(nodos);
                                    duplicates = findDuplicateInStream(pluCodes);
                                }

                                // Obtener atributos (si existen)
                                if (nodo.getNodeName().equals("InfoSPF")) {
                                    NamedNodeMap atributos = nodo.getAttributes();
                                    for (int j = 0; j < atributos.getLength(); j++) {
                                        Node atributo = atributos.item(j);

                                        if (atributo.getNodeName().equals("CodigoPLU")) {
                                            codigoPLU = atributo.getNodeValue();
                                        }
                                        if (atributo.getNodeName().equals("Precio")) {
                                            precio = atributo.getNodeValue();
                                        }
                                        if (atributo.getNodeName().equals("MontoDesc")) {
                                            descuento = atributo.getNodeValue();
                                        }

                                    }

                                    totalDiscount = totalDiscount + Integer.parseInt(descuento);
                                    finalPrice = finalPrice + Integer.parseInt(precio);


                                    if (!codigoPLU.isEmpty() && !precio.isEmpty()) {
                                        if (descuento.isEmpty() || descuento.equals("0")) {
                                            descuento = "0";
                                        }else{
                                            countDiscount++;
                                        }


                                        if (!codigoPLU.equals(lastCodigoPLU)) {

                                            if (isDuplicate(codigoPLU, duplicates)) {
                                                countDuplicates = getCountDuplicates(codigoPLU, pluCodes);
                                                totalPrice = Integer.parseInt(precio);
                                                content.append(getStaticLine(nodos)).append(" ITEM            ").append(codigoPLU.substring(1)).append(" producto,").append(precio).append(",").append(totalPrice * countDuplicates).append(",").append(countDuplicates).append(",1,0,0,0,0,0,0,0,").append(descuento).append("\n");
                                            } else {
                                                content.append(getStaticLine(nodos)).append(" ITEM            ").append(codigoPLU.substring(1)).append(" producto,").append(precio).append(",").append(precio).append(",").append("1,1,0,0,0,0,0,0,0,").append(descuento).append("\n");
                                            }
                                        }
                                        lastCodigoPLU = codigoPLU;

                                    }
                                }

                                if (nodo.getNodeName().equals("Media")) {
                                    NamedNodeMap mediaAttributes = nodo.getAttributes();
                                    for (int j = 0; j < mediaAttributes.getLength(); j++) {
                                        Node mediaAttribute = mediaAttributes.item(j);

                                        if (mediaAttribute.getNodeName().equals("NumTender")) {
                                            numTender = calculateTextNumTender(mediaAttribute.getNodeValue());
                                        }
                                        if (mediaAttribute.getNodeName().equals("MontoTender")) {
                                            tenderAmount = mediaAttribute.getNodeValue();
                                        }

                                    }
                                    content.append(getStaticLine(nodos)).append(" PAYMENT         ").append(numTender).append(",").append(tenderAmount).append(",,,").append("\n");

                                }


                            }
                        }

                        String textDate =calculateTextDate(date);



                        content.append(getStaticLine(nodos)).append(" PROMO_AMOUNT    ").append(totalDiscount).append("\n");
                        content.append(getStaticLine(nodos)).append(" SUBTOTAL        ").append(finalPrice - totalDiscount).append("\n");
                        content.append(getStaticLine(nodos)).append(" RUT_CLIENTE     ").append(rut).append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE").append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE          TOTAL NUM.ITEMS VENDIDOS =    ").append(pluCodes.size()).append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE").append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE       USTED AHORRO HOY!").append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE       -------------------").append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE        TOTAL DESCUENTOS     ").append(countDiscount).append("            ").append(totalDiscount).append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE").append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE").append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE        NOMBRE DEL CAJERO:").append(cashierName).append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE        ").append("C").append(cashierId).append("    ").append("#").append(ticketNumber).append("    ").append(hour).append("    ").append(textDate).append("\n");
                        content.append(getStaticLine(nodos)).append(" TEXT_LINE                   ").append("T0").append(storeNumber).append("    ").append("R00").append(posNumber).append("\n");

                        writeFile(toPath+separator, content.toString(), ticketNumber);

                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private static String calculateTextNumTender(String numTender){

        String descriptionTender = "";
        switch (numTender) {
            case "1":
                descriptionTender = "EFECTIVO";
                break;
            case "2":
                descriptionTender = "CHEQUE";
                break;
            case "3":
                descriptionTender = "Pago CMR Cheque";
                break;
            case "4":
                descriptionTender = "Tarjeta Evento";
                break;
            case "5":
                descriptionTender = "CMR";
                break;
            case "6":
                descriptionTender = "TARJETA CREDITO";
                break;
            case "7":
                descriptionTender = "DEBITO";
                break;
            case "8":
                descriptionTender = "CREDITO FALABELL";
                break;
            case "9":
                descriptionTender = "DEBITO FALABELLA";
                break;
            case "10":
                descriptionTender = "Nota de Credito";
                break;
            case "11":
                descriptionTender = "Ajuste Ley 20956";
                break;
            case "12":
                descriptionTender = "EdenRed";
                break;
            case "13":
                descriptionTender = "GIFTCORP";
                break;
            case "14":
                descriptionTender = "AMIPASS";
                break;
            case "15":
                descriptionTender = "Edenred Dif";
                break;
            case "17":
                descriptionTender = "Avance CMR";
                break;
            case "18":
                descriptionTender = "Pago CMR";
                break;
            case "19":
                descriptionTender = "QuickPayCredito";
                break;
            case "20":
                descriptionTender = "QuickPayDebito";
                break;
            case "21":
                descriptionTender = "GIFTCARD";
                break;
            case "22":
                descriptionTender = "GC ACTIVATION";
                break;
            case "23":
                descriptionTender = "GC DEACTIVATION";
                break;
            case "24":
                descriptionTender = "CREDITO FACTURA";
                break;
            case "25":
                descriptionTender = "FPAY";
                break;
            case "40":
                descriptionTender = "SODEXO";
                break;
            default:
                throw new IllegalArgumentException("Descripción Invalida: " + descriptionTender);
        }
        return descriptionTender;
    }

    private  static String calculateTextDate(String date){


        String year = date.substring(0,4);
        String month = date.substring(4, 6);
        String day =  date.substring(6, 8);



        String textMonth ="";

        switch (month) {
            case "01":
                textMonth = "ENE";
                break;
            case "02":
                textMonth = "FEB";
                break;
            case "03":
                textMonth = "MAR";
                break;
            case "04":
                textMonth = "ABR";
                break;
            case "05":
                textMonth = "MAY";
                break;
            case "06":
                textMonth = "JUN";
                break;
            case "07":
                textMonth = "JUL";
                break;
            case "08":
                textMonth = "AGO";
                break;
            case "09":
                textMonth = "SEP";
                break;
            case "10":
                textMonth = "OCT";
                break;
            case "11":
                textMonth = "NOV";
                break;
            case "12":
                textMonth = "DIC";
                break;
            default:
                throw new IllegalArgumentException("Fecha Invalida: " + textMonth);
        }

        if(day.charAt(0) == '0'){
            day = day.substring(1,2);
        }

        return day+textMonth+year;

    }

    private static void writeFile(String toPath, String content, String fileName){

        try (FileOutputStream fileOutputStream = new FileOutputStream(toPath+fileName+".txt")) {
            fileOutputStream.write(content.getBytes());
            System.out.println("Archivo: "+fileName+".txt generado");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static int getCountDuplicates(String code, List<String> pluCodes){

        int totalCountDuplicates = 0;
        for (String cadena : pluCodes) {
            if(cadena.equals(code)){
                totalCountDuplicates++;
            }
        }
        return totalCountDuplicates;
    }

    private static Boolean isDuplicate(String code, List<String> duplicateCode) {

        Boolean duplicate=false;
        for (int i = 0; i < duplicateCode.size(); i++) {
            if(duplicateCode.get(i).equals(code)){
                duplicate = true;
            }
        }
        return duplicate;

    }

    private static List<String> resolveString(NodeList nodos) {

        List<String> pluCodes = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                if (nodo.getNodeName().equals("InfoSPF")) {
                    NamedNodeMap atributos = nodo.getAttributes();
                    pluCodes.add(saveProductCodes(atributos));
                }
            }
        }
        return pluCodes;
    }

    private static List<String> findDuplicateInStream(List<String> codeList)
    {

        return codeList.stream()
                .collect(Collectors.groupingBy(s -> s))
                .entrySet()
                .stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static String saveProductCodes(NamedNodeMap nodos){

        String code ="";
        for (int i = 0; i < nodos.getLength(); i++) {
            Node atributo = nodos.item(i);
            if(atributo.getNodeName().equals("CodigoPLU")){
                code = atributo.getNodeValue();
            }
        }
        return code;
    }

    private static String getAttribute(NamedNodeMap atributos, String name) {
        String attribute = "";
        for (int j = 0; j < atributos.getLength(); j++) {
            Node atributo = atributos.item(j);
            if (atributo.getNodeName().equals(name)) {
                attribute = atributo.getNodeValue();
            }
        }
        return attribute;
    }

    private static String convertDateWhitOutBars(String reverseDate) {
        String finalDate = "";
        finalDate =reverseDate.substring(0, 4) + reverseDate.substring(5, 7)  + reverseDate.substring(8, 10);
        return finalDate;
    }

    private static String convertStaticDate(String reverseDate) {
        String finalDate = "";
        finalDate =reverseDate.substring(8, 10) +"-"+ reverseDate.substring(5, 7)  +"-"+ reverseDate.substring(0, 4);
        return finalDate;
    }

    private static String getStaticLine(NodeList nodos){

        String staticLine= "";
        String staticDate ="";
        String hour= "";
        for (int i = 0; i < nodos.getLength(); i++) {
            Node nodo = nodos.item(i);
            if (nodo.getNodeType() == Node.ELEMENT_NODE) {

                //Obtengo el Ticket, Fecha, hora
                if(nodo.getNodeName().equals("Frame")){
                    NamedNodeMap atributos = nodo.getAttributes();
                    //Campo Fecha estatico
                    staticDate =convertStaticDate(getAttribute(atributos,"Tail_Fecha"));
                    hour = getAttribute(atributos,"Tail_Hora");
                }
                staticLine = "[BT_17514] "+staticDate+" "+hour;
            }
        }
        return  staticLine;
    }


}



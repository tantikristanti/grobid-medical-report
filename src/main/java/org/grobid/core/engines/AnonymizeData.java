package org.grobid.core.engines;

import org.apache.commons.lang3.StringUtils;
import org.grobid.core.analyzers.GrobidAnalyzer;
import org.grobid.core.data.DataToBeAnonymized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AnonymizeData {
    protected GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();
    protected EngineMedicalParsers parsers;

    // anonymize the document numbers
    public String anonymizeNumber(String idno) {
        StringBuilder idnoNew = new StringBuilder(idno);
        for (int i = 0; i < idno.length(); i++) {
            if (Character.isDigit(idno.charAt(i))) {
                int random_int = (int) Math.floor(Math.random() * (9 - 0 + 1) + 0); // min : 0, max : 9
                // replace character at the specified position
                idnoNew.setCharAt(i, Integer.toString(random_int).charAt(0));
            }
        }
        return idnoNew.toString();
    }

    // anonymize the person names
    public String anonymizePersonName(String persName) {
        String inputFileName = "resources/lexicon/PERSNAME_ANONYM.txt";
        List<String> persNames = new ArrayList<>();
        try (Stream<String> lines = Files.lines(Paths.get(inputFileName))) {
            persNames = lines.collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int min = 0;
        int max = persNames.size();
        StringBuilder newPersName = new StringBuilder();
        if (persNames != null && persNames.size() > 0) {
            String[] names = persName.split(" ");
            for (String name : names) {
                String newName = "";
                //Generate random int value from 0 to the size of the list
                int random_int = (int) Math.floor(Math.random() * (max - min + 1) + min);
                if (name.equals(name.toUpperCase())) { // check if the names is all in uppercase which is the case of French last names
                    newName = persNames.get(random_int).toUpperCase();
                } else {
                    newName = persNames.get(random_int);
                }
                newPersName.append(newName).append(" ");
            }
        }
        return newPersName.toString();
    }

    // anonymize email
    public String anonymizeEmail(String email) {
        List<String> emailSplitByExten = Arrays.asList(email.split("@"));
        String emailBeforeExten = emailSplitByExten.get(0);
        List<String> emailBeforeExtenSplit = Arrays.asList(emailBeforeExten.split("\\."));
        StringBuilder anonymEmail = new StringBuilder();
        for (int i = 0; i < emailBeforeExtenSplit.size(); i++) {
            anonymizePersonName(emailBeforeExtenSplit.get(i));
            anonymEmail.append(anonymizePersonName(emailBeforeExtenSplit.get(i)).trim().toLowerCase());
            if (i < emailBeforeExtenSplit.size() - 1) {
                anonymEmail.append(".");
            }
        }
        anonymEmail.append("@").append(emailSplitByExten.get(1));
        return anonymEmail.toString();
    }

    public List<String> returnNameFromEmail(String email) {
        List<String> emailSplitByExten = Arrays.asList(email.split("@"));
        String names = emailSplitByExten.get(0);
        List<String> namesSplit = Arrays.asList(names.split("\\."));
        List<String> listNames = new ArrayList<>();
        for (int i = 0; i < namesSplit.size(); i++) {
            listNames.add(namesSplit.get(i));
        }
        return listNames;
    }

    // anonymize the address with thise format: zip;city_community
    public List<DataToBeAnonymized> anonymizeAddress(String address) throws Exception {
        List<DataToBeAnonymized> listDataToBeAnonymized = new ArrayList<>();
        DataToBeAnonymized dataToBeAnonymized = new DataToBeAnonymized();
        String newAddress = address;
        String inputFileName = "resources/lexicon/ADDRESS_ANONYM.txt";
        List<String> determinant = Arrays.asList("AU", "AUX", "DU", "DES", "D\\'", "LA", "LE", "LES", "L\\'", "UN", "UNE");
        List<String> dicAddrressList = new ArrayList<>();
        String number = "", newNumber = "", postCode = "", newCityPostCode = "", city = "", newCity = "", newPostCode = "";
        try (Stream<String> lineAddress = Files.lines(Paths.get(inputFileName))) {
            int cityNumToken = 0, newCityNumToken = 0;
            List<String> addressSplit = analyzer.tokenize(address);
            for (int i = 0; i < addressSplit.size(); i++) {
                if (addressSplit.get(i).matches("\\d{2,3}")) { // the street or the building number
                    number = addressSplit.get(i);
                    newNumber = anonymizeNumber(number);
                }
                if (addressSplit.get(i).matches("\\d{5}")) { // the post code
                    postCode = addressSplit.get(i);
                }
            }

            // collect the dictionary of new addresses
            dicAddrressList = lineAddress.collect(Collectors.toList());
            for (int i = 0; i < dicAddrressList.size(); i++) {
                if (dicAddrressList.get(i).contains(postCode)) {
                    city = StringUtils.substringBefore(dicAddrressList.get(i), ";");
                    cityNumToken = analyzer.tokenize(city).size();
                }
            }
            if (city.length() > 0) { // not found post code and city name in the dictionary
                int min = 0;
                int max = dicAddrressList.size();
                int newPosition = (int) Math.floor(Math.random() * (max - min + 1) + min);
                newCityPostCode = dicAddrressList.get(newPosition);
                newCity = StringUtils.substringBefore(newCityPostCode, ";");
                newPostCode = newCityPostCode.substring(newCityPostCode.length() - 5);
                newCityNumToken = analyzer.tokenize(newCity).size();

                boolean containDeterminant = false;
                for (String det : determinant) {
                    if (newCity.contains(det)) {
                        containDeterminant = true;
                        break;
                    }
                }
                while (newCityNumToken != cityNumToken && containDeterminant) { // take only the postal code where city/community name doesn't contain determinant (to avoid errors with body parts)
                    newPosition = (int) Math.floor(Math.random() * (max - min + 1) + min);
                    newCityPostCode = dicAddrressList.get(newPosition);
                    newCity = StringUtils.substringBefore(newCityPostCode, ";");
                    newPostCode = newCityPostCode.substring(newCityPostCode.length() - 5);
                    newCityNumToken = analyzer.tokenize(newCity).size();

                    containDeterminant = false;
                    for (String det : determinant) {
                        if (newCity.contains(det)) {
                            containDeterminant = true;
                            break;
                        }
                    }
                    if (newCityNumToken == cityNumToken && !containDeterminant) {
                        break;
                    }
                }
            }

            if (number.length() > 0 && newNumber.length() > 0) {
                dataToBeAnonymized = new DataToBeAnonymized();
                dataToBeAnonymized.setDataOriginal(number.trim());
                dataToBeAnonymized.setDataPseudo(newNumber.trim());
                listDataToBeAnonymized.add(dataToBeAnonymized);
                newAddress = newAddress.replace(number, newNumber);
            }

            if (postCode.length() > 0 && newPostCode.length() > 0) {
                dataToBeAnonymized = new DataToBeAnonymized();
                dataToBeAnonymized.setDataOriginal(postCode.trim());
                dataToBeAnonymized.setDataPseudo(newPostCode.trim());
                listDataToBeAnonymized.add(dataToBeAnonymized);
                newAddress = newAddress.replace(postCode, newPostCode);
            }

            if (city.length() > 0 && newCity.length() > 0) {
                dataToBeAnonymized = new DataToBeAnonymized();
                dataToBeAnonymized.setDataOriginal(city);
                dataToBeAnonymized.setDataPseudo(newCity);
                listDataToBeAnonymized.add(dataToBeAnonymized);
                newAddress = newAddress.replace(city, newCity);
            }

            if (address.length() > 0 && newAddress.length() > 0) {
                dataToBeAnonymized = new DataToBeAnonymized();
                dataToBeAnonymized.setDataOriginal(address);
                dataToBeAnonymized.setDataPseudo(newAddress);
                listDataToBeAnonymized.add(dataToBeAnonymized);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return listDataToBeAnonymized;
    }

    // anonymize the dates in ISO Format (yyyy-mm-dd)
    public List<String> anonymizeDateISOFormat(String date, String mode) {
        List<String> dateSplit = Arrays.asList(date.split("-"));
        int random_int_date = 1;
        int random_int_month = 1;
        int currentYear = java.time.LocalDate.now().getYear(); // we use the current year for the new data

        if (mode.equals("patient")) {
            int min = 1;
            int max = 200; // assuming patients born 200 years ago (max)
            int random_int = (int) Math.floor(Math.random() * (max - min + 1) + min);
            currentYear = currentYear - random_int;
        }

        List<String> monthList = Arrays.asList("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre");
        String newMM = "", newDD = "";

        // Simply change the year with current year
        dateSplit.set(0, String.valueOf(currentYear));

        // check whether the month is in digit form or not
        String month = dateSplit.get(1);
        if (month.matches("\\d+")) { // the month is in digit form
            random_int_month = (int) Math.floor(Math.random() * (12 - 1 + 1) + 1); // for the month number, min : 1, max : 12
            if (random_int_month == 1 || random_int_month == 3 || random_int_month == 5 || random_int_month == 7 ||
                random_int_month == 8 || random_int_month == 10 || random_int_month == 12) {
                random_int_date = (int) Math.floor(Math.random() * (31 - 1 + 1) + 1); // for the date, min : 1, max : 31
            } else if (random_int_month == 4 || random_int_month == 6 || random_int_month == 9 || random_int_month == 11) {
                random_int_date = (int) Math.floor(Math.random() * (30 - 1 + 1) + 1); // for the date, min : 1, max : 30
            } else if (random_int_month == 2) {
                if (currentYear % 4 == 0) {
                    random_int_date = (int) Math.floor(Math.random() * (29 - 1 + 1) + 1); // for the date, min : 1, max : 29
                } else {
                    random_int_date = (int) Math.floor(Math.random() * (28 - 1 + 1) + 1); // for the date, min : 1, max : 28
                }
            }

            if (random_int_date < 10) {
                newDD = "0" + String.valueOf(random_int_date);
            } else {
                newDD = String.valueOf(random_int_date);
            }
            if (random_int_month < 10) {
                newMM = "0" + String.valueOf(random_int_month);
            } else {
                newMM = String.valueOf(random_int_month);
            }
            dateSplit.set(1, newMM);
            dateSplit.set(2, newDD);

        } else { // the month is not in digit form
            random_int_month = (int) Math.floor(Math.random() * (11 - 1 + 1) + 1); // for the month number, min : 1, max : 12
            if (random_int_month == 0 || random_int_month == 2 || random_int_month == 4 || random_int_month == 6 ||
                random_int_month == 7 || random_int_month == 9 || random_int_month == 11) {
                random_int_date = (int) Math.floor(Math.random() * (31 - 1 + 1) + 1); // for the date, min : 1, max : 31
            } else if (random_int_month == 3 || random_int_month == 5 || random_int_month == 8 || random_int_month == 10) {
                random_int_date = (int) Math.floor(Math.random() * (30 - 1 + 1) + 1); // for the date, min : 1, max : 30
            } else if (random_int_month == 1) {
                if (currentYear % 4 == 0) {
                    random_int_date = (int) Math.floor(Math.random() * (29 - 1 + 1) + 1); // for the date, min : 1, max : 29
                } else {
                    random_int_date = (int) Math.floor(Math.random() * (28 - 1 + 1) + 1); // for the date, min : 1, max : 28
                }
            }
            if (random_int_date < 10) {
                newDD = "0" + String.valueOf(random_int_date);
            } else {
                newDD = String.valueOf(random_int_date);
            }
            dateSplit.set(1, monthList.get(random_int_month));
            dateSplit.set(2, newDD);
        }
        return dateSplit;
    }

    public String anonymizeDateRaw(String date) {
        String newDate = date;
        if (date.matches("^\\d+\\/\\d+\\/\\d{4}$") ||
            date.matches("^\\d+ \\d+ \\d{4}$") ||
            date.matches("^\\d+\\-\\d+\\-\\d{4}$") ||
            date.matches("^\\d+\\.\\d+\\.\\d{4}$") ||
            date.matches("^\\d+ \\D+ \\d{4}$") ||
            date.matches("^\\d+\\.\\D+\\.\\d{4}$")) {
            newDate = anonymizeDate(date);
        }
        return newDate;
    }

    public boolean isContainDigit(String text) {
        boolean digit = false;
        if (text.matches("^\\d+\\/\\d+\\/\\d{4}$") ||
            text.matches("^\\d+ \\d+ \\d{4}$") ||
            text.matches("^\\d+\\-\\d+\\-\\d{4}$") ||
            text.matches("^\\d+\\.\\d+\\.\\d{4}$") ||
            text.matches("^\\d+ \\D+ \\d{4}$") ||
            text.matches("^\\d+\\.\\D+\\.\\d{4}$") ||
            (text.matches("\\d+"))) {
            digit = true;
        }
        return digit;
    }

    // anonymize the dates
    public String anonymizeDate(String date) {
        List<String> tokenizedDate = analyzer.tokenize(date);
        String newDate = "";
        int random_int_date = 1;
        int random_int_month = 1;
        int random_int_year = (int) Math.floor(Math.random() * (110 - 1 + 1) + 1); // for the month number, min : 1, max : 100; we assume that max human age is 110
        int currentYear = java.time.LocalDate.now().getYear(); // we use the current year for the new data
        List<String> monthList = Arrays.asList("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre");
        String newDD = "", newMM = "", newYY = String.valueOf(currentYear - random_int_year);
        // we will process only the date with these formats: "dd-mm-yyyy", "dd/mm/yyyy", "dd mm yyyy", "dd-mm-yyyy"
        // or if the tokenized date contains 5 elements (including the delimiters)
        if (tokenizedDate.get(2) != null) {
            String month = tokenizedDate.get(2);
            random_int_month = (int) Math.floor(Math.random() * (12 - 1 + 1) + 1); // for the month number, min : 1, max : 12

            if (month.matches("\\d+")) { // if the month are all digits
                if (random_int_month < 10) {
                    newMM = "0" + String.valueOf(random_int_month);
                } else {
                    newMM = String.valueOf(random_int_month);
                }
            } else if (month.matches("\\D+")) {
                newMM = monthList.get(random_int_month);
            }

            if (random_int_month == 1 || random_int_month == 3 || random_int_month == 5 || random_int_month == 7 ||
                random_int_month == 8 || random_int_month == 10 || random_int_month == 12) {
                random_int_date = (int) Math.floor(Math.random() * (31 - 1 + 1) + 1); // for the date, min : 1, max : 31
            } else if (random_int_month == 4 || random_int_month == 6 || random_int_month == 9 || random_int_month == 11) {
                random_int_date = (int) Math.floor(Math.random() * (30 - 1 + 1) + 1); // for the date, min : 1, max : 30
            } else if (random_int_month == 2) {
                if (currentYear % 4 == 0) {
                    random_int_date = (int) Math.floor(Math.random() * (29 - 1 + 1) + 1); // for the date, min : 1, max : 29
                } else {
                    random_int_date = (int) Math.floor(Math.random() * (28 - 1 + 1) + 1); // for the date, min : 1, max : 28
                }
            }

            if (random_int_date < 10) {
                newDD = "0" + String.valueOf(random_int_date);
            } else {
                newDD = String.valueOf(random_int_date);
            }

            tokenizedDate.set(0, newDD);
            tokenizedDate.set(2, newMM);
            tokenizedDate.set(4, newYY);
        }
        newDate = String.join("", tokenizedDate);
        return newDate;
    }

    public static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder converted = new StringBuilder();
        boolean convertNext = true;
        for (char ch : text.toCharArray()) {
            if (Character.isSpaceChar(ch)) {
                convertNext = true;
            } else if (convertNext) {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }

        return converted.toString();
    }

    public static void main(String[] args) throws Exception {
        GrobidAnalyzer analyzer = GrobidAnalyzer.getInstance();
        AnonymizeData anonymizeData = new AnonymizeData();

        System.out.println("==============");
        String idno1 = "841606135", idno2 = "19KH01245\n N BN b Wb Wb N b N b WBN b N b Wb N b N BWb Wb N b Wb N b N b N BWb Wb N b N";
        System.out.println("Original idno1: " + idno1 + "; idno2: " + idno2);
        String idnoAnonymized1 = anonymizeData.anonymizeNumber(idno1), idnoAnonymized2 = anonymizeData.anonymizeNumber(idno2);
        System.out.println("Anonymized idno1: " + idnoAnonymized1 + "; idno2: " + idnoAnonymized2);

        System.out.println("==============");
        // anonymize the person names
        String persName = "Mammout KING KONG";
        System.out.println("Original person name: " + persName);
        String persNameAnonymized = anonymizeData.anonymizePersonName(persName);
        System.out.println("Anonymized person name: " + persNameAnonymized);

        System.out.println("==============");
        // anonymize the dates
        String date1 = "11/08/2015", date2 = "01.02.2000", date3 = "03 04 1970", date4 = "1-2-1990", date5 = "1 janvier 1940";
        // List<String> tokenizeDate1 = analyzer.tokenize(date1), tokenizeDate2 = analyzer.tokenize(date2), tokenizeDate3 = analyzer.tokenize(date3), tokenizeDate4 = analyzer.tokenize(date4), tokenizeDate5 = analyzer.tokenize(date5);
        //tokenizeDate1: [11, /, 08, /, 2015]; tokenizeDate2: [01, ., 02, ., 2000]; tokenizeDate2: [03,  , 04,  , 1970]; tokenizeDate4: [1, -, 2, -, 1990]; tokenizeDate5: [1,  , janvier,  , 1940]

        System.out.println("Original date1: " + date1 + "; date2: " + date2 + "; date3: " + date3 + "; date4: " + date4 + "; date5: " + date5);
        String dateAnonymized1 = anonymizeData.anonymizeDateRaw(date1), dateAnonymized2 = anonymizeData.anonymizeDateRaw(date2), dateAnonymized3 = anonymizeData.anonymizeDateRaw(date3), dateAnonymized4 = anonymizeData.anonymizeDateRaw(date4), dateAnonymized5 = anonymizeData.anonymizeDateRaw(date5);
        System.out.println("Anonymized date1: " + dateAnonymized1 + "; date2: " + dateAnonymized2 + "; date3: " + dateAnonymized3 + "; date4: " + dateAnonymized4 + "; date5: " + dateAnonymized5);

        System.out.println("==============");
        // anonymize the email
        String email = "belle.fille@aphp.fr";
        System.out.println("Original email: " + email);
        String emailAnonymized = anonymizeData.anonymizeEmail(email);
        System.out.println("Anonymized email: " + emailAnonymized);

        // anonymize the addresses
        System.out.println("==============");
        String address = "10 RUE ROGER SALENGRO 94270 LE KREMLIN BICETRE";
        System.out.println("Original address: " + address);
        List<DataToBeAnonymized> newAddress = anonymizeData.anonymizeAddress(address);
        System.out.println("Anonym address: " + newAddress.get(0).getDataPseudo());

        // check upper, lower, title case
        System.out.println("==============");
        String str1 = "I Am Fine.", str2 = "I AM FINE.", str3 = "i am fine.";
        System.out.println(str1 + ": " + toTitleCase(str2).equals(str1));
        System.out.println(toTitleCase(str2));
    }
}



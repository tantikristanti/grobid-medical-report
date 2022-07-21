package org.grobid.core.engines;

import org.grobid.core.data.Date;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.grobid.core.document.TEIFormatter.toISOString;

public class AnonymizeData {
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
        for (int i=0; i<emailBeforeExtenSplit.size(); i++) {
            anonymizePersonName(emailBeforeExtenSplit.get(i));
            anonymEmail.append(anonymizePersonName(emailBeforeExtenSplit.get(i)).trim().toLowerCase());
            if (i<emailBeforeExtenSplit.size()-1) {
                anonymEmail.append(".");
            }
        }
        anonymEmail.append("@").append(emailSplitByExten.get(1));
        return anonymEmail.toString();
    }

    // anonymize the person names
    public String anonymizeAddress() {
        String inputFileName = "resources/lexicon/ADDRESS_ANONYM.txt";
        List<String> addresses = new ArrayList<>();
        try (Stream<String> lines = Files.lines(Paths.get(inputFileName))) {
            addresses = lines.collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        int min = 0;
        int max = addresses.size();
        String newAddress = "";
        if (addresses != null && addresses.size() > 0) {
            //Generate random int value from 0 to the size of the list
            int random_int = (int) Math.floor(Math.random() * (max - min + 1) + min);
            newAddress = addresses.get(random_int);
        }
        return newAddress;
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
        String newMM="", newDD="";

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
        StringBuilder newDate = new StringBuilder();
        List<String> originalBirthDateSplit = Arrays.asList(date.split(" "));
        for (int i = 0; i < originalBirthDateSplit.size(); i++) {
            String dateToBeChecked = originalBirthDateSplit.get(i);
            if (dateToBeChecked.matches("^\\d{2}\\/\\d{2}\\/\\d{4}$") ||
                dateToBeChecked.matches("^\\d{2}\\-\\d{2}\\-\\d{4}$") ||
                dateToBeChecked.matches("^\\d{2}\\.\\d{2}\\.\\d{4}$")) {
                originalBirthDateSplit.set(i, anonymizeDate(dateToBeChecked));
            } else { // if not, whatever, just change the number
                originalBirthDateSplit.set(i, anonymizeNumber(originalBirthDateSplit.get(i)));
            }
            newDate.append(originalBirthDateSplit.get(i)).append(" ");
        }

        return newDate.toString();
    }

    // anonymize the dates
    public String anonymizeDate(String date) {
        String newDate = "";
        List<String> dateTokens = new ArrayList<>();
        int random_int_date = 1;
        int random_int_month = 1;
        int currentYear = java.time.LocalDate.now().getYear(); // we use the current year for the new data
        List<String> monthList = Arrays.asList("janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre");
        StringTokenizer dateTok = new StringTokenizer(date, "\\-|\\.|\\/", true);
        while (dateTok.hasMoreTokens()) {
            dateTokens.add(dateTok.nextToken());
        }
        // if the month is not null
        if (dateTokens.get(2) != null) {
            String month = dateTokens.get(2);
            if (month.matches("\\d+")) {
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
                String newDD = "", newMM = "";
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
                dateTokens.set(0, newDD);
                dateTokens.set(2, newMM);
            } else {
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
                String newDD = "";
                if (random_int_date < 10) {
                    newDD = "0" + String.valueOf(random_int_date);
                } else {
                    newDD = String.valueOf(random_int_date);
                }
                dateTokens.set(0, newDD);
                dateTokens.set(2, monthList.get(random_int_month));
            }
        }

        // if the year is not null
        if (dateTokens.get(4) != null) {
            dateTokens.set(4, String.valueOf(currentYear)); // we simply change the yyyy
        }

        newDate = String.join("", dateTokens);

        return newDate;
    }

    public static void main(String[] args) {
        AnonymizeData anonymizeData = new AnonymizeData();

        // anonymize the document numbers
        System.out.println("==============");
        String idno = "841606135";
        System.out.println("Original IDNO: " + idno);
        String idnoAnonymized = anonymizeData.anonymizeNumber(idno);
        System.out.println("Anonymized IDNO: " + idnoAnonymized);

        System.out.println("==============");
        // anonymize the person names
        String persName = "Mammout KING KONG";
        System.out.println("Original person name: " + persName);
        String persNameAnonymized = anonymizeData.anonymizePersonName(persName);
        System.out.println("Anonymized person name: " + persNameAnonymized);

        System.out.println("==============");
        // anonymize the dates
        String date = "11/08/2015";
        System.out.println("Original date: " + date);
        String dateAnonymized = anonymizeData.anonymizeDate(date);
        System.out.println("Anonymized date: " + dateAnonymized);

        System.out.println("==============");
        // anonymize the email
        String email = "belle.fille@aphp.fr";
        System.out.println("Original email: " + email);
        String emailAnonymized = anonymizeData.anonymizeEmail(email);
        System.out.println("Anonymized email: " + emailAnonymized);

        System.out.println("==============");
        // anonymize the raw date
        String rawDate = "Né(e) le : 21/01/1920 (102 ans)";
        System.out.println("Original raw date: " + rawDate);
        String rawDateAnonymized = anonymizeData.anonymizeDateRaw(rawDate);
        System.out.println("Anonymized raw date: " + rawDateAnonymized);
    }
}



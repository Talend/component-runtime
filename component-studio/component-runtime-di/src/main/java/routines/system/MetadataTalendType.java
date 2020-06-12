/**
 * Copyright (C) 2006-2020 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package routines.system;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// CHECKSTYLE:OFF
public class MetadataTalendType {

    private static String MAPPING_FOLDER = "xmlMappings";

    public static String IGNORE_LEN = "ignoreLen";

    public static String IGNORE_PRE = "ignorePre";

    public static String DEFAULT_PRECISION = "defaultPrecision";

    public static String DEFAULT_LENGTH = "defaultLength";

    private static Map<String, Map<String, String>> DB_TO_TALEND_TYPES =
            Collections.synchronizedMap(new HashMap<String, Map<String, String>>());

    private static Map<String, Map<String, String>> TALEND_TO_DB_TYPES =
            Collections.synchronizedMap(new HashMap<String, Map<String, String>>());

    private static Map<String, Map<String, List<String>>> TALEND_TO_DB_TYPES_LIST =
            Collections.synchronizedMap(new HashMap<String, Map<String, List<String>>>());

    private static Map<String, Map<String, Map<String, String>>> DB_TYPTES =
            Collections.synchronizedMap(new HashMap<String, Map<String, Map<String, String>>>());

    private MetadataTalendType() {// protect the class not to be instantiated
        super();
    }

    /**
     * Get children of type ELEMENT_NODE from parent <code>parentNode</code>.
     * 
     * @param parentNode
     * @return
     */
    private static List<Node> getChildElementNodes(Node parentNode) {
        Node childNode = parentNode.getFirstChild();
        ArrayList<Node> list = new ArrayList<Node>();
        while (childNode != null) {
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                list.add(childNode);
            }
            childNode = childNode.getNextSibling();
        }
        return list;
    }

    private static void load(java.io.InputStream file) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        try {
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Throwable e) {
            // do nothing
        }

        DocumentBuilder analyser;
        try {
            analyser = documentBuilderFactory.newDocumentBuilder();

            Document document = analyser.parse(file);

            NodeList dbmsNodes = document.getElementsByTagName("dbms"); //$NON-NLS-1$
            for (int iDbms = 0; iDbms < dbmsNodes.getLength(); iDbms++) {
                Node dbmsNode = dbmsNodes.item(iDbms);
                NamedNodeMap dbmsAttributes = dbmsNode.getAttributes();

                String dbmsIdValue = dbmsAttributes.getNamedItem("id").getNodeValue().toLowerCase(); //$NON-NLS-1$

                if (DB_TO_TALEND_TYPES.containsKey(dbmsIdValue)) {// the corresponding mapping file is already loaded.
                    break;
                }
                Map<String, String> talendToDBMap = Collections.synchronizedMap(new HashMap<String, String>());// use to
                // store--DBType:TalendType
                Map<String, String> dbToTalendMap = Collections.synchronizedMap(new HashMap<String, String>());// use to
                // store--TalendType:DBType
                Map<String, Map<String, String>> dbTypesMaps =
                        Collections.synchronizedMap(new HashMap<String, Map<String, String>>());// use to
                // store
                // DB types

                // use to store--TalendType:DBType and all DBType as List
                Map<String, List<String>> talendToDBList =
                        Collections.synchronizedMap(new HashMap<String, List<String>>());

                DB_TO_TALEND_TYPES.put(dbmsIdValue, talendToDBMap);// store
                TALEND_TO_DB_TYPES.put(dbmsIdValue, dbToTalendMap);
                DB_TYPTES.put(dbmsIdValue, dbTypesMaps);
                TALEND_TO_DB_TYPES_LIST.put(dbmsIdValue, talendToDBList);

                List<Node> childrens = getChildElementNodes(dbmsNode);

                // the dbTypes element--------start
                Node dbTypesNode = childrens.get(0);// the dbTypes element
                List<Node> dbTypesNodes = getChildElementNodes(dbTypesNode);
                for (Node dbTypeNode : dbTypesNodes) {
                    NamedNodeMap dbTypeAttributes = dbTypeNode.getAttributes();

                    Map<String, String> dbTypeMap = Collections.synchronizedMap(new HashMap<String, String>());
                    Node dbType = dbTypeAttributes.getNamedItem("type");
                    String dbTypeValue = null;
                    if (dbType != null) {
                        dbTypeValue = dbType.getNodeValue();
                    }
                    dbTypesMaps.put(dbTypeValue, dbTypeMap);

                    Node dbDefaultLength = dbTypeAttributes.getNamedItem(DEFAULT_LENGTH);
                    String dbDefaultLengthValue = null;
                    if (dbDefaultLength != null) {
                        dbDefaultLengthValue = dbDefaultLength.getNodeValue();
                    }
                    dbTypeMap.put(DEFAULT_LENGTH, dbDefaultLengthValue);

                    Node dbDefaultPrecision = dbTypeAttributes.getNamedItem(DEFAULT_PRECISION);
                    String dbDefaultPrecisionValue = null;
                    if (dbDefaultPrecision != null) {
                        dbDefaultPrecisionValue = dbDefaultPrecision.getNodeValue();
                    }
                    dbTypeMap.put(DEFAULT_PRECISION, dbDefaultPrecisionValue);

                    Node dbIgnoreLength = dbTypeAttributes.getNamedItem(IGNORE_PRE);
                    String dbIgnoreLengthValue = null;
                    if (dbIgnoreLength != null) {
                        dbIgnoreLengthValue = dbIgnoreLength.getNodeValue();
                    }
                    dbTypeMap.put(IGNORE_PRE, dbIgnoreLengthValue);

                    Node dbIgnoreLen = dbTypeAttributes.getNamedItem(IGNORE_LEN);
                    String dbIgnoreLenValue = null;
                    if (dbIgnoreLen != null) {
                        dbIgnoreLenValue = dbIgnoreLen.getNodeValue();
                    }
                    dbTypeMap.put(IGNORE_LEN, dbIgnoreLenValue);
                }
                // the dbTypes element -------end

                childrens = childrens.subList(1, childrens.size());// get all the language nodes
                // find the java language node
                for (int i = 0; i < childrens.size(); i++) {
                    if (!("java").equals(childrens.get(i).getAttributes().getNamedItem("name").getNodeValue())) {
                        continue;
                    }

                    Node javaNode = childrens.get(i);
                    List<Node> mappingDirections = getChildElementNodes(javaNode);

                    // this part is for Talend to DB mapping---------BEGIN
                    Node talendToDBTypes = mappingDirections.get(0);// the element:TalendToDBTypes
                    List<Node> talendTypeSourcesList = getChildElementNodes(talendToDBTypes);
                    for (Node talendTypeSource : talendTypeSourcesList) {
                        NamedNodeMap talendTypeAttributes = talendTypeSource.getAttributes();
                        Node talendTypeItem = talendTypeAttributes.getNamedItem("type"); //$NON-NLS-1$
                        if (talendTypeItem == null) {
                            continue;
                        }
                        String talendType = talendTypeItem.getNodeValue();
                        List<Node> languageTypesNodes = getChildElementNodes(talendTypeSource);

                        List<String> dbList = Collections.synchronizedList(new ArrayList<String>()); // new a dbType
                        // list

                        boolean defaultSelected = false;

                        for (int j = 0; j < languageTypesNodes.size(); j++) {

                            Node languageTypeNode = languageTypesNodes.get(j);

                            NamedNodeMap dbTypeAttributes = languageTypeNode.getAttributes();

                            Node dbTypeItem = dbTypeAttributes.getNamedItem("type"); //$NON-NLS-1$
                            if (talendTypeItem == null) {
                                continue;
                            }
                            String dbType = dbTypeItem.getNodeValue();

                            Node defaultSelectedItem = dbTypeAttributes.getNamedItem("default"); //$NON-NLS-1$
                            if (!defaultSelected) {
                                defaultSelected = defaultSelectedItem != null
                                        && defaultSelectedItem.getNodeValue().equalsIgnoreCase("true") ? true : false; //$NON-NLS-1$
                                if (defaultSelected == true) {// store the default talendType for the DB type.
                                    dbToTalendMap.put(talendType, dbType);
                                    // break;
                                }
                            }

                            dbList.add(dbType);// add this dbType name to list

                        }
                        // no default value, then use the first one as the talend type
                        if (!defaultSelected) {
                            if (languageTypesNodes.size() > 0) {
                                Node dbTypeItem = languageTypesNodes.get(0).getAttributes().getNamedItem("type");
                                if (dbTypeItem != null) {
                                    dbToTalendMap.put(talendType, dbTypeItem.getNodeValue());
                                }
                            } else {// there is no talend type
                                dbToTalendMap.put(talendType, null);
                            }
                        }

                        talendToDBList.put(talendType, dbList);// store the list to talendToDBList

                    }
                    // Talend to DB mapping -----END

                    // this part is for DB to Talend Type mapping -----BEGIN
                    Node dbToTalendTypes = mappingDirections.get(1);// the element:dbToTalendTypes
                    List<Node> dbTypeSourcesList = getChildElementNodes(dbToTalendTypes);

                    int dbTypeSourcesListListSize = dbTypeSourcesList.size();
                    for (int iDbTypeSource = 0; iDbTypeSource < dbTypeSourcesListListSize; iDbTypeSource++) {
                        Node dbTypeSource = dbTypeSourcesList.get(iDbTypeSource);

                        NamedNodeMap dbTypeAttributes = dbTypeSource.getAttributes();
                        Node dbTypeItem = dbTypeAttributes.getNamedItem("type"); //$NON-NLS-1$
                        if (dbTypeItem == null) {
                            continue;
                        }
                        String dbType = dbTypeItem.getNodeValue();

                        List<Node> languageTypesNodes = getChildElementNodes(dbTypeSource);

                        boolean defaultSelected = false;

                        for (int j = 0; j < languageTypesNodes.size(); j++) {

                            Node languageTypeNode = languageTypesNodes.get(j);

                            NamedNodeMap talendTypeAttributes = languageTypeNode.getAttributes();

                            Node talendTypeItem = talendTypeAttributes.getNamedItem("type"); //$NON-NLS-1$
                            if (talendTypeItem == null) {
                                continue;
                            }
                            String talendType = talendTypeItem.getNodeValue();

                            Node defaultSelectedItem = talendTypeAttributes.getNamedItem("default"); //$NON-NLS-1$

                            defaultSelected = defaultSelectedItem != null
                                    && defaultSelectedItem.getNodeValue().equalsIgnoreCase("true") ? true : false; //$NON-NLS-1$
                            if (defaultSelected == true) {// store the default talendType for the DB type.
                                talendToDBMap.put(dbType, talendType);
                                break;
                            }
                        }
                        // no default value, then use the first one as the talend type
                        if (!defaultSelected) {
                            if (languageTypesNodes.size() > 0) {
                                Node talendTypeItem = languageTypesNodes.get(0).getAttributes().getNamedItem("type");
                                if (talendTypeItem != null) {
                                    talendToDBMap.put(dbType, talendTypeItem.getNodeValue());
                                }
                            } else {// there is no talend type
                                talendToDBMap.put(dbType, "id_String");
                            }
                        }
                    }
                    // DB to Talend Type mapping------END
                }

            }

        } catch (ParserConfigurationException e) {
            throw new Exception(e);
        } catch (SAXException e) {
            throw new Exception(e);
        } catch (IOException e) {
            throw new Exception(e);
        }

    }

    private static void loadAll() {
        synchronized (DB_TO_TALEND_TYPES) {
            if (DB_TO_TALEND_TYPES.isEmpty()) {
            }
        }
    }

    // before calling this method, please call getDefaultSelectedTalendType or getDefaultSelectedDbType first
    public static String getDefaultDBTypes(String dbmsId, String dbmsType, String strMap) {
        loadAll();
        Map<String, Map<String, String>> dbmsMap = DB_TYPTES.get(dbmsId.toLowerCase());
        if (dbmsMap != null && !dbmsMap.isEmpty()) {
            Map<String, String> dbmsTypeMap = dbmsMap.get(dbmsType.toUpperCase());
            if (dbmsTypeMap != null && !dbmsTypeMap.isEmpty()) {
                return dbmsTypeMap.get(strMap);
            }
        }

        return null;

    }

    /**
     * 
     * get the Talend type according to the DB id and DB type, so far, length and precision doesn't work.
     * 
     * @author talend
     * @param dbmsId: the db id like "mysql_id"
     * @param dbmsType: the DB type like "VARCHAR"
     * @param length
     * @param precison
     * @return
     */
    public static String getDefaultSelectedTalendType(String dbmsId, String dbmsType, int length, int precison) {
        if (dbmsId == null || "".equals(dbmsId) || dbmsType == null || "".equals(dbmsType)) {
            return "id_String";
        }

        loadAll();

        Map<String, String> mapping = DB_TO_TALEND_TYPES.get(dbmsId.toLowerCase());
        if (mapping != null) {
            for (String dbType : getSynonym(dbmsId, dbmsType)) {
                String talendType = mapping.get(dbType);
                if (talendType != null) {
                    return talendType;
                }
            }
        }

        return "id_String";
    }

    private static List<String> getSynonym(String dbmsId, String dbmsType) {
        List<String> result = new ArrayList<String>(2);
        result.add(dbmsType);
        if ("oracle_id".equals(dbmsId)) {
            if ("TIMESTAMPTZ".equals(dbmsType)) {
                result.add("TIMESTAMP WITH TIME ZONE");
            }
        }
        return result;
    }

    /**
     * 
     * Search and return the Db type which matches with the given parameters. If the Db type is not found, a new search
     * is done with inverse of given <code>nullable</code>
     * 
     * @author talend
     * @param dbmsId: the db id like "mysql_id"
     * @param talendType: the DB type like "id_String"
     * @param length
     * @param precison
     * @return
     */
    public static String getDefaultSelectedDbType(String dbmsId, String talendType, int length, int precison) {
        if (dbmsId == null || "".equals(dbmsId) || talendType == null || "".equals(talendType)) {
            return null;
        }

        loadAll();

        if (TALEND_TO_DB_TYPES.get(dbmsId.toLowerCase()) != null) {
            return TALEND_TO_DB_TYPES.get(dbmsId.toLowerCase()).get(talendType);
        }

        return null;
    }

    /**
     * Search and return the Db type list which matches with the given parameters.
     * 
     * @author talend
     * @param dbmsId: the db id like "mysql_id"
     * @param talendType: the DB type like "id_String"
     * @return
     */
    public static List<String> getTalendToDBList(String dbmsId, String talendType) {
        if (dbmsId == null || "".equals(dbmsId) || talendType == null || "".equals(talendType)) {
            return null;
        }

        loadAll();

        if (TALEND_TO_DB_TYPES_LIST.get(dbmsId.toLowerCase()) != null) {
            return TALEND_TO_DB_TYPES_LIST.get(dbmsId.toLowerCase()).get(talendType);
        }

        return null;
    }
}

/** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 *    ProjectName ZOHO_CRM
 *    File Name   HanderAccouts.java 
 * ** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 *    Copyright (c) 2016 Darlen . All Rights Reserved. 
 *    注意： 本内容仅限于XXX公司内部使用，禁止转发
 * ** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 * */
package darlen.crm.record;

import darlen.crm.jaxb_xml_object.Products.FL;
import darlen.crm.jaxb_xml_object.Products.Response;
import darlen.crm.jaxb_xml_object.Products.Result;
import darlen.crm.jaxb_xml_object.Products.Row;
import darlen.crm.jaxb_xml_object.utils.JaxbUtil;
import darlen.crm.model.result.Products;
import darlen.crm.model.result.User;
import darlen.crm.util.*;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.*;

/**
 * 暂时在程序中不处理删除数据，只处理更新或者添加数据
 * ====================Ⅰ：或者ZOHO xml并组装到zohoMap(id,lastEditTime)中： 参考（JaxbAccountsTest.java/JaxbSOTest.java/JaxbLeadsTest.java）
 * 1. 从ZOHO获取有效的xml
 * 2. xml 转 java bean
 * 3. 组装 zohoListObj ，其中里面的element有：
 * zohoIDMap<ERPID,ZOHOID> = zohoListObj.get(0)
 * zohoTimeMap<ERPID,lastEditTime> = zohoListObj.get(1)
 * delZOHOIDList:里面是所有 ERP ID 为空时的 ZOHO ID
 *
 *注意：
 * 1.ERPID，当ERPID为空时，把
 * 2.ZOHOID一定不能为空
 * 3.lastEditTime
 *
 * ===================Ⅱ：获取db数据，组装成 dbAcctList  : buildDBObjList()
 * 1.Accounts --> dbAcctList.get(0)
 * 2.idAccountsMap<CustomerID,Accounts> --> dbAcctList.get(1)
 *
 * ===================Ⅲ：组装需要真正需要传输到ZOHO的Account对象集合（判断zoho的 id 和 lastEditTime 是否有被修改）
 * 1.如果zohoid存在于dbModel中，则判断 lastEditTime是否被修改，如果修改了，则直接组装dbModel为xml并调用ZOHO中的更新API：updateAccountMap
 * 2.如果zohoid不存在于dbModel中，则直接调用ZOHO删除API：delZOHOIDList
 * 3.如果dbModel中的id不存在于zohoMap中，则组装dbModel为xml并调用Zoho中的添加API：addAccountMap
 *
 *
 * ===================Ⅳ(assembleZOHOXmlTest)：组装XML
 *
 * 重点在getRowByMap方法
 * getRowByMap: getDBFieldNameValueMap()+getZOHOFieldMap()+getAllFLsByCRMMap()
 * 1. dbFieldNameValueMap ： 获取每个Accounts对应的  dbFieldNameValueMap = getDBFieldNameValueMap<dbFieldName,FiledValue>
 * 2. zohoFieldNameValueMap <zohoFieldName,dbFiledValue> --> getZOHOFieldMap()： 根据 dbRdAccountsFieldMapping.properties 过滤 dbFieldNameValueMap,  形成zohoFieldNameValueMap <zohoFieldName,FiledValue>
 * 3. getAllFLsByCRMMap() --> 获得zohoFieldNameValueMap形成的List<FL>
 *
 * ========================Ⅴ：发送xml data到ZOHO，并执行更新、添加或者删除操作
 * 更新（testUpdateAcctRecord）
 * 添加（testAddAcctRecord）
 * 删除（testDelAcctRecord）
 *
 * 注意账号信息：qq:85333000000071039, tree3170:85333000000071001
 * Description：ZOHO_CRM
 * Created on  2016/09/16 21：22
 * -------------------------------------------------------------------------
 * 版本     修改时间        作者         修改内容 
 * 1.0.0        21：22   Darlen              create
 * -------------------------------------------------------------------------
 *
 * @author Darlen liu
 */
public class HandleProduct extends Module{
    private static HandleProduct handleProduct;
    private static Logger logger =  Logger.getLogger(HandleModules.class);
    /**
     * BeforeClass 与Before的区别
     */
    @BeforeClass
    public static void getInstance(){
        handleProduct = new HandleProduct();
        handleProduct.getProperties();
    }

    /**
     * Ⅰ：这里仅仅只是组装zohoAcctObjList
     * 1. 从ZOHO获取有效的xml
     * 2. xml 转 java bean
     * 3. 组装 zohoListObj ，其中里面的element有：
     * zohoIDMap<ERPID,ZOHOID> = zohoListObj.get(0)
     * zohoTimeMap<ERPID,lastEditTime> = zohoListObj.get(1)
     * delZOHOIDList:里面是所有 ERP ID 为空时的 ZOHO ID
     */
    @Test
    public void testAssembleZOHOAcctObjList() throws Exception {
        handleProduct.buildSkeletonFromZohoList();
    }
    public List buildSkeletonFromZohoList() throws Exception {
//        1. 从ZOHO获取有效的xml
        String zohoURL = zohoPropsMap.get(Constants.FETCH_PRODUCTS_URL);//"https://crm.zoho.com.cn/crm/private/xml/Products/getRecords";
        String selectedColumns = "Products(Modified Time,PRODUCTID,Product Name,ERP ID,LatestEditTime)";
        String sortOrderString = "desc";
        String sortColumnString = "Modified Time";
        //注意：format 一定要为2，因为有可能需要的字段为空
        String zohoStr = ModuleUtils.retrieveZohoRecords(zohoURL, Module.NEWFORMAT_2, selectedColumns, sortOrderString, sortColumnString);
//       2. xml 转 java bean
        logger.debug("zohoStr:::\n" + zohoStr);
        Response response = JaxbUtil.converyToJavaBean(zohoStr, Response.class); //response.getResult().getLeads().getRows().get(0).getFls().get(1).getFl()
        logger.debug("response object:::\n" + response);
//     3. 组装 zohoAcctObjList
        List  zohoModuleList = new ArrayList();
        Map<String,String> erpZohoIDMap = new HashMap<String, String>();
        Map<String,String> erpZohoIDTimeMap = new HashMap<String, String>();
        List delZOHOIDList = new ArrayList();
        //如果没有数据<response uri="/crm/private/xml/Products/getRecords"><nodata><code>4422</code><message>There is no data to show</message></nodata></response>
        if(null != response.getResult()){
            List<Row> rows = response.getResult().getProducts().getRows();
            zohoModuleList = handleProduct.buildZohoComponentList(rows, "PRODUCTID", "ERP ID");
            erpZohoIDMap = (Map)zohoModuleList.get(0);
            erpZohoIDTimeMap = (Map)zohoModuleList.get(1);
            delZOHOIDList = (List)zohoModuleList.get(2);
            CommonUtils.printMap(erpZohoIDMap,"ERPID 和ZOHOID Map");
            CommonUtils.printMap(erpZohoIDTimeMap,"ERPID 和LastEditTime Map");
            CommonUtils.printList(delZOHOIDList,"Remove ZOHO ID list");

        }else{
            logger.debug("没有数据了：：：\n"+zohoStr);
            zohoModuleList.add(erpZohoIDMap);
            zohoModuleList.add(erpZohoIDTimeMap);
            zohoModuleList.add(delZOHOIDList);
        }
        return zohoModuleList;
    }
    /**
     * Ⅱ：这里组装db中的AcctObjList
     * 1.Accounts --> dbAcctList.get(0)
     * 2.idAccountsMap<CustomerID,Accounts> --> dbAcctList.get(1)
     */
    @Test
    public void testAssembleDBAcctObjList() throws ParseException {
        handleProduct.buildDBObjList();
    }
    public List buildDBObjList() throws ParseException {
        List dbAcctList = new ArrayList();
        Map<String,Products> erpIDProductsMap = new HashMap<String, Products>();
        getDBObj(erpIDProductsMap);
        getDBObj2(erpIDProductsMap);
        dbAcctList.add(erpIDProductsMap);
        CommonUtils.printList(dbAcctList, "Build DB Object :::");
        return dbAcctList;
    }

    /**
     * Ⅲ：由获得的ZOHO所有对象集合和从DB获取的对象集合，经过过滤，获取的组装需要***发送到ZOHO的对象集合骨架***
     */
    @Test
    public void testAssembelSendToZOHOAcctList() throws Exception {
        handleProduct.build2ZohoObjSkeletonList();
    }

    /**
     * 由获得的ZOHO所有对象集合和从DB获取的对象集合，经过过滤，获取的组装需要***发送到ZOHO的对象集合骨架***
     * 1.updateAccountMap<>：如果Zoho ID存在于DB对象集合中，则判断 lastEditTime是否被修改，如果修改了，则直接组装到updateAccountMap中
     * 2.delZOHOIDList：如果zohoid不存在于dbModel中，则直接调用ZOHO删除API：
     * 3.addAccountMap：如果dbModel中的id不存在于zohoMap中，则组装dbModel为xml并调用Zoho中的添加API：
     * @return
     */
    public List build2ZohoObjSkeletonList() throws Exception {
        //1. 获取ZOHO对象的骨架集合
        List allZohoObjList = buildSkeletonFromZohoList();
        //Map<ERPID，ZOHOID>
        Map<String,String> erpZohoIDMap = (Map)allZohoObjList.get(0);
        Map<String,String> erpIDTimeMap = (Map)allZohoObjList.get(1);
        List<String> delZohoIDList = (List)allZohoObjList.get(2);
        //2.组装DB 对象List
        List dbAcctList = buildDBObjList();
        Map<String,Products> idProductsMap = (Map<String,Products>)dbAcctList.get(0);

        //3. 解析并组装addMap、updateMap、delZohoIDList
        Map<String,Products> addMap = new HashMap<String, Products>();
        Map<String,Products> updateMap = new HashMap<String, Products>();

        for(Map.Entry<String,String> entry : erpIDTimeMap.entrySet()){
            String erpID = entry.getKey();
            if(idProductsMap.containsKey(erpID)){//update
                updateMap.put(erpZohoIDMap.get(erpID), idProductsMap.get(erpID));
            }else{ //delete
                if(!delZohoIDList.contains(erpZohoIDMap.get(erpID))){
                    delZohoIDList.add(erpZohoIDMap.get(erpID));
                }
            }
        }

        for(Map.Entry<String,Products> entry : idProductsMap.entrySet()){
            String dbID = entry.getKey();
            if(!erpIDTimeMap.containsKey(dbID)){//add
                addMap.put(dbID, entry.getValue());
            }
        }

        List sendToZohoList = new ArrayList();
        CommonUtils.printMap(addMap,"addSOMap组装到ZOHO的对象的集合：：：\n");
        sendToZohoList.add(addMap);
        CommonUtils.printMap(updateMap, "updateSOMap组装到ZOHO的对象的集合：：：\n");
        sendToZohoList.add(updateMap);
        CommonUtils.printList(delZohoIDList, "delZOHOSOIDList组装到ZOHO的对象的集合：：：\n");
        sendToZohoList.add(delZohoIDList);

        return sendToZohoList;
    }

    /**
     * Ⅳ：由发送到ZOHO的骨架对象，组装发送到ZOHO 的XML，分别为添加、更新、删除三个对象集合
     * addZOHOXml:最大100条数据
     * updateZOHOXml：一次只能更新1条
     * deleteZOHOIDsList：转换为以逗号分割ZOHO ID的字符串
     */
    @Test
    public void testAssembleZOHOXml() throws Exception {
        handleProduct.build2ZohoXmlSkeleton();
    }

    /**
     * 由发送到ZOHO的骨架对象，组装发送到ZOHO 的XML，分别为添加、更新、删除三个对象集合
     * List<String> addZohoXmlList :每一百条数据组装成xml放入list里面
     * Map<zohoID,zohoXML> updateZOHOXmlMap ：以zohoID为key，xml为value
     * deleteZOHOIDsList： zohoID的集合
     * @return  zohoComponentList
     * @throws Exception
     */
    public List build2ZohoXmlSkeleton() throws Exception {
//        1. 获取发送到ZOHO对象集合骨架
        List zohoComponentList = build2ZohoObjSkeletonList();
        Map<String,Products> addMap =  (Map<String,Products> )zohoComponentList.get(0);
        Map<String,Products> updateMap =(Map<String,Products> )zohoComponentList.get(1);

        //TODO add最大条数为100，
//        2. 添加
        logger.debug("begin组装 AddZOHOXML...\n");
        List<String> addZohoXmlList = buildAdd2ZohoXml(addMap);
        logger.debug("end组装 AddZOHOXML..size:::."+addZohoXmlList.size());

//        3. 更新
        logger.debug("begin组装 updateZOHOXml...\n");
        Map<String,String> updateZOHOXmlMap  = buildUpd2ZohoXml(updateMap);
        logger.debug("end组装 updateZOHOXml...size:::"+updateZOHOXmlMap.size());

        List zohoXMLList = new ArrayList();
        zohoXMLList.add(addZohoXmlList);
        zohoXMLList.add(updateZOHOXmlMap);
//        4. 删除
        List deleteZOHOIDsList  = (List)zohoComponentList.get(2);
        logger.debug("打印删除ZohoIDs集合 deleteZOHOIDsList...\n"+org.apache.commons.lang.StringUtils.join(deleteZOHOIDsList,","));
        zohoXMLList.add(deleteZOHOIDsList);//org.apache.commons.lang.StringUtils.join(deleteZOHOIDsList,",")
        return zohoXMLList;
    }

    /**
     * ========================Ⅴ：发送xml data到ZOHO，并执行更新、添加或者删除操作
     * 更新（testUpdateAcctRecord）
     * 添加（testAddAcctRecord）
     * 删除（testDelAcctRecord）
     */
    @Test
    public void testAddRecords(){
        try {
            String targetURL_Accounts = zohoPropsMap.get(Constants.INSERT_PRODUCTS_URL);
            List<String> addZohoXMLList = (List<String> ) build2ZohoXmlSkeleton().get(0);
            for(int i = 0; i < addZohoXMLList.size(); i ++){
                System.err.println("添加第"+(i+1)+"条数据，xml为："+addZohoXMLList.get(i));
                Map<String,String> postParams = new HashMap<String, String>();
                postParams.put(Constants.HTTP_POST_PARAM_TARGETURL,targetURL_Accounts);
                postParams.put(Constants.HTTP_POST_PARAM_XMLDATA,addZohoXMLList.get(i));
                postParams.put(Constants.HTTP_POST_PARAM_AUTHTOKEN,AUTHTOKEN);
                postParams.put(Constants.HTTP_POST_PARAM_SCOPE, SCOPE);
                postParams.put(Constants.HTTP_POST_PARAM_NEW_FORMAT, NEWFORMAT_1);

                CommonUtils.executePostMethod(postParams);
            }

        } catch(Exception e) {
            logger.error("执行更新Module操作出现错误",e);
        }
    }
    @Test
    public void testUpdateRecords(){
        try {
            String targetURL_Accounts = "https://crm.zoho.com.cn/crm/private/xml/Products/updateRecords";
            //TODO: qq:85333000000071039, tree3170:85333000000071001
            Map<String,String> updZohoXMLMap = (Map<String,String>) build2ZohoXmlSkeleton().get(1);
            int i = 1 ;
            for(Map.Entry<String,String> zohoIDUpdXmlEntry : updZohoXMLMap.entrySet()){
                System.err.println("更新第"+(i)+"条数据，xml为："+zohoIDUpdXmlEntry.getValue());
                Map<String,String> postParams = new HashMap<String, String>();
                postParams.put(Constants.HTTP_POST_PARAM_ID,zohoIDUpdXmlEntry.getKey());
                postParams.put(Constants.HTTP_POST_PARAM_TARGETURL,targetURL_Accounts);
                postParams.put(Constants.HTTP_POST_PARAM_XMLDATA,zohoIDUpdXmlEntry.getValue());
                postParams.put(Constants.HTTP_POST_PARAM_AUTHTOKEN,AUTHTOKEN);
                postParams.put(Constants.HTTP_POST_PARAM_SCOPE, SCOPE);
                postParams.put(Constants.HTTP_POST_PARAM_NEW_FORMAT, NEWFORMAT_1);

                CommonUtils.executePostMethod(postParams);
                i++;
            }
        } catch(Exception e) {
            logger.error("执行更新Module操作出现错误",e);
        }
    }

    /**
     * 根据accountMap 组装成每100条数据的addZohoXmlList中
     * 注意：getAddRowsMap
     *
     * @param accountMap
     * @return
     * @throws Exception
     */
    private List<String> buildAdd2ZohoXml(Map<String, Products> accountMap) throws Exception {
        List<String> addZohoXmlList= new ArrayList<String>();
        Response response = new Response();
        Result result = new Result();
        darlen.crm.jaxb_xml_object.Products.Products products = new darlen.crm.jaxb_xml_object.Products.Products();
        Map<Integer,List<Row>> addRowsMap = getAddRowsMap(accountMap);
        if(addRowsMap==null || addRowsMap.size() == 0){
            return addZohoXmlList;
        }else{

            for(int i = 0 ; i< addRowsMap.size(); i ++){
                products.setRows(addRowsMap.get(i));
                result.setProducts(products);
                response.setResult(result);
                String str  = JaxbUtil.convertToXml(response);
                addZohoXmlList.add(str);
            }
        }
        return addZohoXmlList;
    }

    /**
     * 根据accountMap 组装成updateZphoXmlMap <zohoID,updateXml>
     * @param accountMap
     * @return
     * @throws Exception
     */
    private Map<String,String> buildUpd2ZohoXml(Map<String, Products> accountMap) throws Exception {
        Map<String,String> updateZphoXmlMap = new HashMap<String, String>();
        String str = "";
        Response response = new Response();
        Result result = new Result();
        darlen.crm.jaxb_xml_object.Products.Products products = new darlen.crm.jaxb_xml_object.Products.Products();
        List<Row> rows = getUpdRowByMap(accountMap);
        if(rows==null || rows.size() == 0){
            return updateZphoXmlMap;
        }else{
            int i = 0;
            for (Map.Entry<String,Products> zohoIDAccountEntry : accountMap.entrySet()){
                products.setRows(Arrays.asList(rows.get(i)));
                result.setProducts(products);
                response.setResult(result);
                logger.debug("组装更新的第"+(i+1)+"条数据：：：");
                str = JaxbUtil.convertToXml(response);
                updateZphoXmlMap.put(zohoIDAccountEntry.getKey(),str);
                i++;
            }

        }
        return updateZphoXmlMap;
    }

    /**
     * ************注意这里需要处理普通的FL和product detail的FL
     * 获取Row对象的集合 : getDBFieldNameValueMap() + getZOHOFieldMap() + getAllFLsByCRMMap()
     * 1. dbFieldNameValueMap ： 获取每个Accounts对应的  dbFieldNameValueMap = getDBFieldNameValueMap<dbFieldName,FiledValue>
     * 2. zohoFieldNameValueMap <zohoFieldName,dbFiledValue> --> getZOHOFieldMap()： 根据dbRdAccountsFieldMapping.properties 过滤 dbFieldNameValueMap,  形成zohoFieldNameValueMap <zohoFieldName,FiledValue>
     * 3. getAllFLsByCRMMap() --> 获得zohoFieldNameValueMap形成的List<FL>
     * @param accountMap
     * @return
     * @throws Exception
     */
    private List<Row> getUpdRowByMap(Map<String, Products> accountMap) throws Exception {
        List<Row> rows = new ArrayList<Row>();

        int i = 1;
        for(Map.Entry<String,Products> entry : accountMap.entrySet()){
            Row row = new Row();
            String key = entry.getKey();
            Products products  = entry.getValue();
            List fls = getAllFLList(products);
            row.setNo(i);
            row.setFls(fls);
            rows.add(row);
            i++;
        }
        return rows;
    }

    /**
     * 获取Add的row的Map： 每100条rows放入Map，最后不满100条rows的放到最后的Map中
     * @param accountMap
     * @return
     * @throws Exception
     */
    private Map<Integer,List<Row>> getAddRowsMap(Map<String, Products> accountMap) throws Exception {
        List<Row> rows = new ArrayList<Row>();
        Map<Integer,List<Row>>  rowsMap = new HashMap<Integer, List<Row>>();
        int i = 1;
        for(Map.Entry<String,Products> entry : accountMap.entrySet()){
            Row row = new Row();
            String key = entry.getKey();
            Products products  = entry.getValue();
            List fls = getAllFLList(products);
            row.setNo(i);
            row.setFls(fls);
            rows.add(row);
            //当row的size达到了100，那么需要放入
            if(i == Constants.MAX_ADD_SIZE){
                logger.debug("Add Rows的size达到了100，需要放到Map中，然后重新计算rows的条数...");
                rowsMap.put(rowsMap.size(),rows);
                rows = new ArrayList<Row>();
                i = 1;
            }else{
                i++;
            }

        }
        //最后不满100条的，放入最后的Map中,如果刚好则不添加
        if(rows.size()>0) rowsMap.put(rowsMap.size()+1,rows);
        return rowsMap;
    }
    /**
     * 获取所有FL的集合，返回的List中存在2大对象：commonFls，products
     *  1. commonFls --> Common FL 集合
     *
     * @param products
     * @return
     * @throws Exception
     */
    private List getAllFLList(Products products) throws Exception {
        //通过反射拿到Products对应的所有ERP字段
        Map<String,Object> dbFieldNameValueMap = ModuleUtils.getDBFieldNameValueMap("darlen.crm.model.result.Products", products);
        // 通过properties过滤不包含在里面的所有需要发送的ZOHO字段
        List zohoFieldList = getZOHOFLsByProps(CommonUtils.readProperties("/mapping/dbRdProductsFieldMapping.properties"), dbFieldNameValueMap);
        return zohoFieldList;
    }


    /**
     * 根据properties和有效的dbFieldNameValueMap确定返回给zoho的fieldname（获取properties中的key对应的value）和fieldvalue
     * @param properties
     * @param dbFieldNameValueMap
     * @return
     */
    private List<FL> getZOHOFLsByProps(Properties properties, Map dbFieldNameValueMap) {
        logger.debug("开始properties的过滤...");
        List<FL> fls = new ArrayList<FL>();

        for(Map.Entry entry : properties.entrySet()){
            if(dbFieldNameValueMap.containsKey(entry.getKey())){
                FL fl = new FL();
                fl.setFieldName((String)entry.getValue());
                fl.setFieldValue((String)dbFieldNameValueMap.get(entry.getKey()));
                fls.add(fl);
            }
        }
        CommonUtils.printList(fls, "过滤后的 所有FL的集合:");
        return fls;

    }
    /**
     * 获取DB某个Accounts所有有效的fieldname 和value的Map
     * @param className 包名+类名
     * @return
     * refer:http://blog.csdn.net/sd4000784/article/details/7448221
     */
    public static Map<String,Object> getDBFieldNameValueMap(String className,Object dbFields) throws Exception {
        Map<String,Object> map = new HashMap();
        Class clazz = Class.forName(className);
        Field[] fields = clazz.getDeclaredFields();
        Method[] methods = clazz.getMethods();
        for(Field field : fields){
            String fieldName = field.getName();
            field.setAccessible(true) ;
            if (field.getGenericType().toString().equals("class java.lang.String")
                    ||"".equals("class darlen.crm.model.result.User") ) {// 如果type是类类型，则前面包含"class "，后面跟类名
                String fieldValue =String.valueOf(field.get(dbFields));
                if(!StringUtils.isEmptyString(fieldValue)){
                    map.put(fieldName,fieldValue);
                }
            }else if(field.getGenericType().toString().equals("class darlen.crm.model.result.User")){//处理User对象:拥有者
                map.putAll(getDBFieldNameValueMap("darlen.crm.model.result.User", field.get(dbFields)));
            }
        }
        CommonUtils.printMap(map,"打印DBfield的map");
        return map;
    }

    /**
     * TODO: 注意其中一些字段该放入值
     * 1. 注意productid 和product name一定要存在与系统中
     * 2. 注意PRODUCTSID是系统生成的，不需要设入
     * 3.
     * @param idProductsMap
     * @return
     */
    private Products getDBObj(Map<String, Products> idProductsMap) throws ParseException {
        Products products = new Products();
        products.setErpID("5");
        // 产品名称
        products.setProdName("尼龍背心環保袋");
        //设置Product Owner产品拥有者：与lastEditBy一致
//        User user = new User();
//        user.setUserID("85333000000071039");
//        user.setUserName("qq");
        User user = ModuleUtils.fetchDevUser(false);
        products.setUser(user);
        products.setEnabled("true");
        //产品分类
        products.setCatagory("Paper fan /Plastic fan/Bamboo fan" );
        //产品子分类
        products.setSubCategory("Paper gifts & memo pad");
        //ItemDesc产品描述
        products.setItemDesc("Promotion plastic fan");
        //Unit单位
        products.setUnit("pcs");
        //barcode
        products.setBarcode("");
        products.setRemark("長方形移動電源");
        products.setLatestEditBy(user.getUserName());
        String currentDate = ThreadLocalDateUtil.formatDate(new Date());
        products.setLatestEditTime(currentDate);
        idProductsMap.put(products.getErpID(),products);
        return products;
    }

    private Products getDBObj2(Map<String, Products> idProductsMap) throws ParseException {
        Products products = new Products();
        products.setErpID("2");
        // 产品名称
        products.setProdName("水松木杯墊");
        //设置Product Owner产品拥有者：与lastEditBy一致
//        User user = new User();
//        user.setUserID("85333000000071039");
//        user.setUserName("qq");
        User user = ModuleUtils.fetchDevUser(false);
        products.setUser(user);
        products.setEnabled("true");
        products.setLatestEditBy(user.getUserName());
        String currentDate = ThreadLocalDateUtil.formatDate(new Date());
        products.setLatestEditTime(currentDate);
        idProductsMap.put(products.getErpID(),products);
        return products;
    }

    /**
     * 获取Zoho组件的集合，其中包含三个对象，分别为 erpZohoID，erpIDTime，delZohoIDList（zoho ID list）
     * 1. erpZohoID<erpID,zohoID> = zohoListObj.get(0)
     * 2. erpIDTime<erpID,lastEditTime> = zohoListObj.get(1)
     * 3. delZohoIDList
     * @param rows
     * @param zohoIDName
     * @param erpIDName ERP ID-->
     * @return  zohoCompList
     */
    private List buildZohoComponentList(List<Row> rows, String zohoIDName, String erpIDName) {
        List  zohoCompList = new ArrayList();

        Map<String,String> erpZohoIDMap = new HashMap<String, String>();
        Map<String,String> erpIDTimeMap = new HashMap<String, String>();
        List delZohoIDList = new ArrayList();
        zohoCompList.add(erpZohoIDMap);
        zohoCompList.add(erpIDTimeMap);
        zohoCompList.add(delZohoIDList);

        for (int i = 0; i < rows.size() ; i++){
            logger.debug("遍历第"+(i+1)+"条数据:::"+rows.get(i));
            String zohoID = "";
            String erpID = "";
            String lastEditTime = "";
            List<FL> fls = rows.get(i).getFls();
            boolean hasERPID = true;
            for(FL fl : fls){
                String fieldName = fl.getFieldName();
                String fieldVal = fl.getFieldValue();
                if(zohoIDName.equals(fieldName) && !StringUtils.isEmptyString(fieldVal)){
                    zohoID = fieldVal;
                }
                if(erpIDName.equals(fieldName)){
                    if(StringUtils.isEmptyString(fieldVal)){
                        fieldVal = "emptyERPID_"+i;
                        hasERPID = false;
                    }
                    erpID = fieldVal;
                    //如果出现重复的erpID，那么删除其中一条
                    if(erpZohoIDMap.containsKey(erpID)){
                        hasERPID = false;
                        erpID = "dulERPID_"+erpID+"_"+i;
                    }
                }
                if("LatestEditTime".equals(fieldName)){
                    lastEditTime = fieldVal;
                }
            }
            erpZohoIDMap.put(erpID, zohoID);
            erpIDTimeMap.put(erpID, lastEditTime);
            //如果ERPID为空，那么加入到删除列表中
            if(!hasERPID) delZohoIDList.add(zohoID);
        }
        return zohoCompList;
    }

}

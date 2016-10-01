/** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 *    ProjectName ZOHO_CRM
 *    File Name   DBUtils.java 
 * ** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 *    Copyright (c) 2016 Darlen . All Rights Reserved. 
 *    注意： 本内容仅限于XXX公司内部使用，禁止转发
 * ** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 * */
package darlen.crm.util;

import darlen.crm.manager.ConfigManager;
import darlen.crm.model.result.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.Date;

/**
 * darlen.crm.util
 * TODO
 * 1. DB Object设置好【done】
 * 2. 13个人设置好【done】
 * 3. 当last edit time与ZOHO的不一致才去修改
 *
 *
 * 0. 编写configuration类
 *
 * 1. 测试4个Module是否已经ok
 * 2 .测试13个人的账号是否ok
 *
 * Description：ZOHO_CRM
 * Created on  2016/09/30 21：37
 * -------------------------------------------------------------------------
 * 版本     修改时间        作者         修改内容 
 * 1.0.0        21：37   Darlen              create
 * -------------------------------------------------------------------------
 *
 * @author Darlen liu
 */
public class DBUtils {
    private static Logger logger = Logger.getLogger(DBUtils.class);
//    public static Map<String,String> zohoDBPropsMap = new HashMap<String, String>();
//    public static Map<String,String> accountPropsMap = new HashMap<String, String>();

    /**
     * 初始化ZOHO配置文件中的一些字段值
     */
//    private synchronized static void initZohoProps() {
//        try {
//            Properties prop = new Properties();
//            prop.load(DBUtils.class.getResourceAsStream("/secure/db.properties"));
            //for url TODO  将来需要把properties中的字段全部放入到Cache或者静态变量中
//            for(Map.Entry entry : prop.entrySet()){
//                zohoDBPropsMap.put(String.valueOf(entry.getKey()),String.valueOf(entry.getValue()));
//            }
//            prop = new Properties();
//            prop.load(DBUtils.class.getResourceAsStream("/mapping/Accounts.properties"));
//            for(Map.Entry entry : prop.entrySet()){
//                accountPropsMap.put(String.valueOf(entry.getKey()),String.valueOf(entry.getValue()));
//            }
//        } catch(IOException e) {
//            logger.error("读取zoho properties出现错误", e);
//        }
//        CommonUtils.printMap(zohoDBPropsMap,"db.properties的value：：：");
//        CommonUtils.printMap(accountPropsMap,"db.properties的value：：：");
//    }

    /**
     * 获取DB连接
     */
    public static Connection getConnection () throws IOException, ConfigurationException {
        String driverName = ConfigManager.get(Constants.PROPS_DB_2,"DB_DRIVER_NAME");//zohoDBPropsMap.get("DB_DRIVER_NAME");//"com.microsoft.sqlserver.jdbc.SQLServerDriver";  //加载JDBC驱动
        String dbURL =ConfigManager.get(Constants.PROPS_DB_2,"DB_URL");//zohoDBPropsMap.get("DB_URL"); //"jdbc:sqlserver://localhost:1433; DatabaseName=test";  //连接服务器和数据库test
        String userName = ConfigManager.get(Constants.PROPS_DB_2,"DB_USERNAME");//zohoDBPropsMap.get("DB_USERNAME");;//"sa";  //默认用户名
        String userPwd = ConfigManager.get(Constants.PROPS_DB_2,"DB_PWD");//zohoDBPropsMap.get("DB_PWD");//"zaq1@WSX";  //密码
        Connection dbConn = null;

        try {
            Class.forName(driverName);
            dbConn = DriverManager.getConnection(dbURL, userName, userPwd);
            logger.debug("Connection Successful!");  //如果连接成功 控制台输出Connection Successful!
        } catch (Exception e) {
            logger.error("连接SQL SERVER失败",e);
        }
        return dbConn;
    }

    public static void main(String[] args) throws Exception {
        getConnection();
//        CommonUtils.printList(getAccountList(), "打印所有的Customer：：：");
//        CommonUtils.printList(getProductList(), "打印所有的 Products：：：");
//        getSOList();
//        getInvoiceList();
//        CommonUtils.printList(getSOList(), "打印所有的销售订单：：：");
    }

    /**
     * 如果ZOHO所有的用户账号中包含LastEditBy，则返回true，否则返回false
     * @param lastEditBy
     * @return
     */
    private static boolean containERPAcct(String lastEditBy) throws IOException, ConfigurationException {
        //accountPropsMap.containsKey(lastEditBy)
        if(!StringUtils.isEmptyString(lastEditBy) && "".equals(ConfigManager.getAcctfromProps(lastEditBy))){
            return true;
        }
        return false;
    }

    /**
     * TODO
     * 1. 在此之前，需要先过滤13个账户
     * 2. 从Cache中拿LastEditBy的Name和ID
     * @return
     * @throws SQLException
     * @throws ParseException
     */
    public synchronized static List<Accounts> getAccountList(Map<String,Object> dbIDModuleObjMap) throws Exception {
        List<Accounts> accountList = new ArrayList<Accounts>();
        String sql = "select * from dbo.Customer " +
                "where CustomerID in (1,8,20)"; //暂时只用三条数据
        ResultSet rs = exeQuery(sql);
        while (rs != null && rs.next()){
            String lastEditBy = rs.getString("LatestEditBy");
            if(containERPAcct(lastEditBy)){
                Accounts account = new Accounts();
                // ERP ID
                String erpID = rs.getString("CustomerID");
                account.setErpID(erpID);
                //TODO:从Cache中拿到Name，并查到ID，然后填上ID和Name作为拥有者设入。如果Name改变了怎么办
                //User --> lastEditBy一致
                //User --> lastEditBy一致
//                User user = new User("80487000000076001","marketing");
                User user = new User(ConfigManager.getAcctfromProps(lastEditBy),lastEditBy);
                account.setUser(user);
                //客戶編號
                account.setCustomerNO(rs.getString("CustomerRef"));
                //客户公司名
                account.setAcctName(rs.getString("Name"));
                //TODO: 数据库中Enable是1或者0，但是在ZOHO中是true或者false，需要转换下
                //是否隱藏客户            JDBC BIT 类型的 Java 映射的推荐类型是 Java 布尔型:
                boolean enable = rs.getBoolean("Enabled");
                account.setEnabled(String.valueOf(enable));
                account.setPhone(rs.getString("Tel"));
                account.setFax(rs.getString("Fax"));
                //公司聯絡人
                account.setContact(rs.getString("Contact"));
                //聯絡人直線电话
                account.setDirect(rs.getString("Direct"));
                //发货地址
                account.setDeliveryAddress(rs.getString("DeliveryAddress"));
                account.setEmail(rs.getString("Email"));
                //邮寄地址
                account.setMailAddress(rs.getString("MailAddress"));
                //客户网站
                account.setWebsite(rs.getString("Website"));
                //国家ID (他們沒有用此功能)
    //            account.setCountryID(rs.getString("CountryID"));
                //州
                account.setState(rs.getString("state"));
                //邮编
                account.setPostNo(rs.getString("PostNo"));
                //城市
                account.setCity(rs.getString("City"));
                //配送方式
                account.setDeliveryMethod(rs.getString("DeliveryMethod"));
                //備註
                account.setRemark(rs.getString("Remark"));
                account.setCreationTime(rs.getString("CreationTime"));
                String creationTime = ThreadLocalDateUtil.formatDate(rs.getTimestamp("CreationTime"));
                //TODO 暂时测试阶段用最新的时间
                boolean isDevMod  = "1".equals(ConfigManager.get(Constants.PROPS_ZOHO_1,Constants.ZOHO_PROPS_DEV_MODE));
                String lastEditTime = isDevMod ? ThreadLocalDateUtil.formatDate(new Date()):ThreadLocalDateUtil.formatDate(rs.getTimestamp("LatestEditTime"));
                account.setCreationTime(creationTime);
                account.setLatestEditTime(lastEditTime);

                account.setLatestEditBy(lastEditBy);
                accountList.add(account);
                dbIDModuleObjMap.put(erpID,account);
            }
        }
        return accountList;
    }

    public synchronized static List<Products> getProductList(Map<String,Object> dbIDModuleObjMap) throws Exception {
        List<Products> productsList = new ArrayList<Products>();
        String sql = "select * from dbo.item " +
                "where itemid in (6,9,10,130)"; //暂时只用三条数据
        ResultSet rs = exeQuery(sql);
        while (rs != null && rs.next()){
            String lastEditBy = rs.getString("LatestEditBy");
            if(containERPAcct(lastEditBy)){
                Products product = new Products();
                String erpID = rs.getString("ItemID");
                product.setErpID(erpID);
                // Product Name 产品名称
                product.setProdName(rs.getString("Name"));
                //Product Code产品编码
                product.setProdCode(rs.getString("ItemRef"));
                //设置Product Owner产品拥有者：与lastEditBy一致
                //TODO：Product Owner产品所有者 ，就是DB中的lastEditBy
                User user = new User();
                user.setUserID(ConfigManager.getAcctfromProps(lastEditBy));//accountPropsMap.get(lastEditBy)
                user.setUserName(lastEditBy);
                //User user = fetchDevUser(false);
                product.setUser(user);
                //是否隱藏客户
                product.setEnabled(rs.getString("Enabled"));
                //产品分类
                product.setCatagory(rs.getString("Catagory"));
                //产品子分类
                product.setSubCategory(rs.getString("SubCategory"));
                //ItemDesc产品描述
                product.setItemDesc(rs.getString("Description"));
                //Unit单位
                product.setUnit(rs.getString("Unit"));
                //barcode條碼
                product.setBarcode(rs.getString("Barcode"));
                product.setRemark(rs.getString("Remark"));
                product.setLatestEditBy(lastEditBy);
                boolean isDevMod  = "1".equals(ConfigManager.get(Constants.PROPS_ZOHO_1,Constants.ZOHO_PROPS_DEV_MODE));
                String latestEditTime = isDevMod ? ThreadLocalDateUtil.formatDate(new Date()):ThreadLocalDateUtil.formatDate(rs.getTimestamp("LatestEditTime"));
//                String latestEditTime = ThreadLocalDateUtil.formatDate(rs.getTimestamp("LatestEditTime"));
                product.setLatestEditTime(latestEditTime);
                productsList.add(product);
                dbIDModuleObjMap.put(erpID,product);
            }
        }
        logger.debug("Product size:::" + productsList.size());
        return productsList;
    }

    /**
     * TODO:不是直接顯示ID，要顯示PaymentTerm表中的Name字段
     * 1. 重点在处理各种ID和Name
     * 2. 重点在Product Detail的处理：处理多个Product和1个Product情况
     * @return
     * @throws SQLException
     * @throws ParseException
     */
    public synchronized static List<SO> getSOList(Map<String,Object> dbIDModuleObjMap) throws Exception{
        List<SO> moduleList = new ArrayList<SO>();
        String sql = "select s1.soid , s1.cusName,s2.soid ,s2.Item_SOID,s2.ItemID,s2.ItemName from  item_SO s2  left join  SO s1 on s1.soid = s2.SOID";
        sql = "select s1.soid , s1.cusName,s2.Item_SOID,s2.ItemID,s2.ItemName, item.Name as itemName\n" +
                " from SO s1 \n" +
                "left join  item_SO s2  on s1.soid = s2.SOID\n" +
                "left join  item item  on s2.itemid = item.itemid\n" +
                " order by s1.soid\n";
        sql = "SELECT so.SOID AS ERPID, so.CUSNAME AS CUSTOMERNAME, so.EXCHANGERATE AS EXGRATE ,\n" +
                "itemso.ITEM_SOID,item.ITEMID AS PROD_ID, item.NAME AS PROD_NAME, itemso.SOPRICE AS PROD_UNITPRICE,  itemso.QUANTITY AS PROD_QUANTITY, itemso.ITEMDISCOUNT AS PROD_DISCOUNT, itemso.DESCRIPTION AS PROD_DESC , \n" +
                "so.*\n" +
                " FROM SO so\n" +
                "LEFT JOIN  ITEM_SO itemso  ON so.SOID = itemso.SOID\n" +
                "LEFT JOIN  ITEM item  ON itemso.itemid = item.ITEMID\n" +
                "where so.SOID in (13,16,27)\n" + //暂时只用三条数据
                " ORDER BY SO.SOID";
        ResultSet rs = exeQuery(sql);
        String preErpID = "";
        List<ProductDetails> pds = new ArrayList<ProductDetails>();
        SO so = null;
        while (rs != null && rs.next()){
            String lastEditBy = rs.getString("LatestEditBy");
            if(containERPAcct(lastEditBy)){
                /**DB中的SO id*/
                String curErpID = rs.getString("SOID");
                //相同SO，代表这个SO有多个Product，不需要新创建SO，只需要把product添加到已有的List<ProductDetails> pds中
                if(preErpID.equals(curErpID) && so != null){
                    /**
                     * 处理list<ProductDetail>, 根据db中的SOID关联Item_SO表，拿出所有的product Detail,注意为空情况
                     Double.valueOf(so.getErpExchangeRate())
                     */
                    pds.add(assembleProduct(rs));
                    so.setPds(pds);
                } else{//代表不同SO,需要重新创建SO对象
                    //erpid 不相同之前，先把上一个SO添加到list中（排除第一条数据对第一次遍历SO）
                    if(null != so )moduleList.add(so);
                    //因为是不同的SO对象了，所以把SO添加到list之后，需要重新创建SO对象, 并且把preErpID指向当前的ERP ID
                    so = new SO();
                    preErpID = curErpID;
                    so.setErpID(curErpID);
                    //TODO 销售编号与SoNumber需要确认,不需要，这个字段是ZOHO ID
    //                so.setSALESORDERID(rs.getString("ItemRef"));
                    //TODO 销售拥有者
            //        so.setOwerID("85333000000071039");
            //        so.setOwner("qq");
//                    User user = new User("80487000000076001","marketing");
                    User user = new User(ConfigManager.getAcctfromProps(lastEditBy),lastEditBy);
                    so.setUser(user);
                    so.setSubject(rs.getString("SORef"));
                    /**ZOHO生成的字段，似乎没什么作用*/
                    so.setSoNumber("ItemRef");
                    /**
                     * TODO
                     * Account id 和Name一般是同时存在的；
                     * 如果只存在id，Name可以不对；
                     * 如果只存在Name，那么会新创建一个客户；
                     * 例外：但如果有ID，那么ID必需存在已经创建的客户中
                     */
                    so.setACCOUNTID(rs.getString("CustomerID"));//"80487000000096005"
                    /**
                     * 客户名Account Name：PriPac Design & Communication AB, 注意&符号，以后会改成CDATA形式
                     */
                    so.setAcctName(rs.getString("CusName"));//"永昌紙品"
                    //报价名称（查找类型）
                    so.setQuoteNO(rs.getString("QuoteRef"));
                    //客户名ERP_Currency,DB中用CurrencyName表示
                    so.setErpCurrency(rs.getString("CurrencyName"));
                    //公司联络人Contact
                    so.setContact(rs.getString("CusContact"));
                    //客户邮件地址
                    so.setMailAddress(rs.getString("CusMailAddress"));
                    so.setEmail(rs.getString("CusEmail"));
                    //客户PONo
                    so.setPoNO(rs.getString("CustomerPONo"));
                    so.setDeliveryAddress(rs.getString("CusDeliveryAddress"));
                    so.setTel(rs.getString("CusTel"));
                    so.setFax(rs.getString("CusFax"));
                    BigDecimal exchangeRate = rs.getBigDecimal("ExchangeRate");
                    so.setErpExchangeRate(exchangeRate.toString());
                    /**TODO:不是直接顯示ID，要顯示PaymentTerm表中的Name字段         */
                    so.setPaymentTerm(rs.getString("PaymentTermID"));
                    so.setPayMethod(rs.getString("PayMethod"));
                    so.setDeliveryMethod(rs.getString("DeliveryMethod"));
                    so.setPaymentPeriod(rs.getString("PaymentPeriod"));
                    //销售订单日期SODate
                    String SODate = ThreadLocalDateUtil.formatDate(rs.getTimestamp("SODate"));
                    so.setErpDueDate(SODate);
                    boolean isDevMod  = "1".equals(ConfigManager.get(Constants.PROPS_ZOHO_1,Constants.ZOHO_PROPS_DEV_MODE));
                    String latestEditTime = isDevMod ? ThreadLocalDateUtil.formatDate(new Date()):ThreadLocalDateUtil.formatDate(rs.getTimestamp("LatestEditTime"));
//                    String currentDate = ThreadLocalDateUtil.formatDate(new Date());
                    so.setLatestEditTime(latestEditTime);
//                    so.setCreationTime("2016-09-01 10:10:10");
                    so.setLatestEditBy(user.getUserName());

                    /**
                     * TODO  , 需要计算： 待定
                     * 设置product detail右下角那堆属于SO的字段:Discount,Sub Total,Grand Total
                     */
                    so.setDiscount("1");
                    so.setSubTotal("1");//小计
                    so.setGrandTotal("1");//累计
                    //Customer Discount == Discount
                    BigDecimal cusDiscount = rs.getBigDecimal("CusDiscount");
                    so.setCusDiscount(String.valueOf(cusDiscount.multiply(exchangeRate)));
                    /**Discount来自销售订单中的“折扣” */
                    so.setDiscount(String.valueOf(cusDiscount.multiply(exchangeRate)));
                    /**Sub Total 来自销售订单中的“小计”*/
                    //TODO : 小计-->估计是total,但是在SO中没有Total，那么就需要自己去计算
                    //so.setSubTotal(String.valueOf(total.multiply(exgRate)));
                    /**Grand Total来自销售订单中的“累计”*/
                    //TODO : 累计-->估计应该是total-折扣
                    //so.setGrandTotal(String.valueOf(total.multiply(exgRate).subtract(cusDiscount)));
                    //处理第一条Product
                    pds.add(assembleProduct(rs));
                    so.setPds(pds);
                    dbIDModuleObjMap.put(curErpID,so);
                }
            }
        }
        return moduleList;
    }



    public synchronized static List<Invoices> getInvoiceList(Map<String,Object> dbIDModuleObjMap) throws Exception{
        List<Invoices> moduleList = new ArrayList<Invoices>();
        String sql = "SELECT inv.InvoiceID AS ERPID, inv.CUSNAME AS CUSTOMERNAME, inv.EXCHANGERATE AS EXGRATE ,\n" +
                "item_inv.Item_InvoiceID,item.ITEMID AS PROD_ID, item.NAME AS PROD_NAME, item_inv.InvoicePrice AS PROD_UNITPRICE,  item_inv.QUANTITY AS PROD_QUANTITY, item_inv.ITEMDISCOUNT AS PROD_DISCOUNT, item_inv.DESCRIPTION AS PROD_DESC , \n" +
                "inv.*\n" +
                " FROM Invoice inv\n" +
                "LEFT JOIN  ITEM_INVOICE item_inv  ON inv.InvoiceID = item_inv.InvoiceID\n" +
                "LEFT JOIN  ITEM item  ON item_inv.itemid = item.ITEMID\n" +
                "where inv.InvoiceID in (8,12,145)\n" + //暂时只用三条数据
                " ORDER BY inv.InvoiceID";
        ResultSet rs = exeQuery(sql);
        String preErpID = "";
        Invoices invoices = null;
        List<ProductDetails> pds = new ArrayList<ProductDetails>();
        while (rs != null && rs.next()){
            String lastEditBy = rs.getString("LatestEditBy");
            if(containERPAcct(lastEditBy)){
                String curErpID = rs.getString("InvoiceID");
                //相同ERPID，代表这个Module有多个Product，不需要新创建Module，只需要把product添加到已有的List<ProductDetails> pds中
                if(preErpID.equals(curErpID) && invoices != null){
                    /**
                     * 处理list<ProductDetail>, 根据db中的SOID关联Item_SO表，拿出所有的product Detail,注意为空情况
                     Double.valueOf(so.getErpExchangeRate())
                     */
                    pds.add(assembleProduct(rs));
                    invoices.setPds(pds);
                } else{//代表不同SO,需要重新创建SO对象
                    //erpid 不相同之前，先把上一个Module添加到list中（排除第一条数据对第一次遍历Module）
                    if(null != invoices )moduleList.add(invoices);
                    //因为是不同的SO对象了，所以把SO添加到list之后，需要重新创建Module对象, 并且把preErpID指向当前的ERP ID
                    invoices = new Invoices();
                    preErpID = curErpID;
                    /**DB中的Invoices id*/
                    invoices.setErpID(curErpID);
                    //Subject主题
                    invoices.setInvoiceSubject(rs.getString("InvoiceRef"));
                    //TODO 1. 注意拥有者User一定要存在系统中  , 发票拥有者Invoice Owner
//                    User user = new User("80487000000076001","marketing");
                    User user = new User(ConfigManager.getAcctfromProps(lastEditBy),lastEditBy);
    //            User user = fetchDevUser(false);
                    invoices.setUser(user);
    //            Invoice Date发货单日期
                    String invoiceDate = ThreadLocalDateUtil.formatDate(rs.getTimestamp("InvoiceDate"));
                    invoices.setInvoiceDate(invoiceDate);
                    /**
                     * TODO：2. 注意SO中的SALESORDERID与Sales Order一定要存在系统
                     * 是否只需要SONo
                     */
                    //so id 和SO name
                    //invoices.setSALESORDERID(rs.getString("PaymentPeriod"));
                    invoices.setSoName(rs.getString("SORef"));
                    //汇率
                    BigDecimal exgRate = rs.getBigDecimal("ExchangeRate");
                    invoices.setErp_ExchangeRate(String.valueOf(exgRate));
                    /**TODO 不是直接顯示ID，要顯示PaymentTerm表中的Name字段         */
                    invoices.setPaymentTerm(rs.getString("PaymentTermID"));
                    invoices.setCustomerNo(rs.getString("CusRef"));
                    //TODO 似乎没用到Due Date到期日期
                    String dueDate = ThreadLocalDateUtil.formatDate(rs.getTimestamp("invoiceDate"));
                    invoices.setDueDate(dueDate);
                    /**
                     * 3. 注意Account一定要存在系统
                     * Account id 和Name一般是同时存在的；
                     * 如果只存在id，Name可以不对；
                     * 如果只存在Name，那么会新创建一个客户；
                     * 例外：但如果有ID，那么ID必需存在已经创建的客户中
                     */
                    invoices.setACCOUNTID(rs.getString("CustomerID"));
                    /**
                     * PriPac Design & Communication AB, 注意&符号，以后会改成CDATA形式
                     */
                    invoices.setAcctName(rs.getString("CusName"));
                    //Contact公司聯絡人
                    invoices.setContact(rs.getString("CusContact"));
                     invoices.setEmail(rs.getString("CusEmail"));
                    //DeliveryAddress客户发货地址
                    invoices.setDeliveryAddress(rs.getString("CusDeliveryAddress"));
                    //客户邮寄地址
                    invoices.setMailAddress(rs.getString("CusMailAddress"));
                    invoices.setFax(rs.getString("CusFax"));
                    invoices.setTel(rs.getString("CusTel"));
                    invoices.setErp_Currency(rs.getString("CurrencyName"));
                    //付款方式
                    invoices.setPayMethod(rs.getString("PayMethod"));
                    //deliveryMethod
                    invoices.setDeliveryMethod(rs.getString("DeliveryMethod"));
                    //客户PONo
                    invoices.setPoNO(rs.getString("CustomerPONo"));
                    //DNNo 送貨單編號
                    invoices.setDnNo(rs.getString("DNNo"));
                    /**TODO Deposit訂金
                     * 匯入時x ExchangeRate換成港幣
                     * */
                     BigDecimal deposit = rs.getBigDecimal("Deposit");
                     invoices.setDeposit(String.valueOf(deposit.multiply(exgRate)));
                    /**其他费用
                     *   匯入時x ExchangeRate換成港幣
                     * */
                    BigDecimal otherCharge = rs.getBigDecimal("OtherCharge");
                    invoices.setOtherCharge(String.valueOf(otherCharge.multiply(exgRate)));
                    /**FreightAmount 运费
                     * 匯入時x ExchangeRate換成港幣
                     * */
                    BigDecimal freightAmount = rs.getBigDecimal("FreightAmount");
                    invoices.setFreightAmount(String.valueOf(freightAmount.multiply(exgRate)));
                    /**
                     * 匯入時x ExchangeRate換成港幣
                     */
                    BigDecimal total = rs.getBigDecimal("Total");
                    invoices.setTotal(String.valueOf(total.multiply(exgRate)));
                    boolean isDevMod  = "1".equals(ConfigManager.get(Constants.PROPS_ZOHO_1,Constants.ZOHO_PROPS_DEV_MODE));
                    String latestEditTime = isDevMod ? ThreadLocalDateUtil.formatDate(new Date()):ThreadLocalDateUtil.formatDate(rs.getTimestamp("LatestEditTime"));
//                    String lastEditTime = ThreadLocalDateUtil.formatDate(rs.getTimestamp("LatestEditTime"));
                    invoices.setLatestEditTime(latestEditTime);
    //            String dueDate = ThreadLocalDateUtil.formatDate(rs.getTimestamp("invoiceDate"));
    //            invoices.setCreationTime("2016-09-01 10:10:10");
                    invoices.setLatestEditBy(user.getUserName());


                    /**
                     * 设置product detail右下角那堆属于Invoices的字段:Discount,Sub Total,Grand Total
                     */
                    /**
                     * TODO : 是百分比还是已经是计算出来的数值，暂时按数值计算
                     * Customer Discount客户折扣来自Customer的折扣  实际上Discount == cusDiscount
                     * */
                    BigDecimal cusDiscount = rs.getBigDecimal("CusDiscount");
                    invoices.setCusDiscount(String.valueOf(cusDiscount.multiply(exgRate)));
                    /**Discount来自销售订单中的“折扣” */
                   invoices.setDiscount(String.valueOf(cusDiscount.multiply(exgRate)));
                    /**Sub Total 来自销售订单中的“小计”*/
                    //TODO : 小计-->估计是total
                    invoices.setSubTotal(String.valueOf(total.multiply(exgRate)));
                    /**Grand Total来自销售订单中的“累计”*/
                    //TODO : 累计-->估计应该是total-折扣
                    invoices.setGrandTotal(String.valueOf(total.multiply(exgRate).subtract(cusDiscount)));
                    pds.add(assembleProduct(rs));
                    invoices.setPds(pds);
                    moduleList.add(invoices);
                    dbIDModuleObjMap.put(curErpID,invoices);
                }
            }

        }
        return moduleList;
    }

    /**
     *  根据db中的SOID关联Item_SO表，拿出所有的product Detail,这里假设找到2条数据
     * @param rs
     * @return
     */
    private static ProductDetails assembleProduct(ResultSet rs) throws SQLException {
//        List<ProductDetails> pds = new ArrayList<ProductDetails>();
        ProductDetails pd =new ProductDetails();
        String erpID = rs.getString("erpID");
        String prodID = rs.getString("PROD_ID");
        String prodName = rs.getString("PROD_NAME");
        BigDecimal unitPrice = rs.getBigDecimal("PROD_UNITPRICE");
        BigDecimal exchangeRate = rs.getBigDecimal("EXGRATE");
        String realUnitPrice = unitPrice == null?"":String.valueOf(unitPrice.multiply(exchangeRate));
        String listPrice = unitPrice == null?"":String.valueOf(unitPrice.multiply(exchangeRate));
        BigDecimal quantity = rs.getBigDecimal("PROD_QUANTITY");
        BigDecimal itemDiscount = rs.getBigDecimal("PROD_DISCOUNT");
        String prodDesc = rs.getString("PROD_DESC");
        BigDecimal total = quantity.multiply(exchangeRate);
        BigDecimal netTotal = total.subtract(total.multiply(itemDiscount));
        logger.debug("打印Product：ERPID="+erpID+", prodID = "+prodID+", prodName = "+prodName +", unitPrice=" +
                ""+unitPrice+", exchangeRate="+exchangeRate+", realUnitPrice="+realUnitPrice+
                ", listPrice= "+listPrice+", quantity="+quantity+", itemDiscount="+itemDiscount+
                ", prodDesc = "+prodDesc+", total="+total+", netTotal="+netTotal);
        /**
         * 注意 product id和Name一定要是已经存在与产品里面的,item ID 和ItemName
         */
        pd.setPd_Product_Id(prodID);
        //产品名字Product Name
        //TODO 需要找ken确认ItemName为【Name】是表示什么意思？是否根据id从Item表找--》Name
        pd.setPd_Product_Name(prodName);
        //定价 (￥)：单价Unit Price
        pd.setPd_Unit_Price(realUnitPrice);//定价  ： DB-->SOPrice,注意价格要跟Currency一致
        //List Price单价
        pd.setPd_List_Price(listPrice);//单价  ： DB-->SOPrice,注意价格要跟Currency一致
        //数量
        pd.setPd_Quantity(String.valueOf(quantity.toString()));//数量
        pd.setPd_Discount(String.valueOf(itemDiscount));//折扣
        //税，Matrix默认这个字段是0，因为不用税
        pd.setPd_Tax("0");
        pd.setPd_Product_Description(prodDesc);
        //金额 = 数量*定价
        pd.setPd_Total(String.valueOf(total));//金额
        //总计=金额-折扣(金额*%折扣)
        pd.setPd_Net_Total(String.valueOf(netTotal));//总计
        return pd;
    }



    private static ResultSet exeQuery(String sql) throws SQLException, IOException, ConfigurationException {
        ResultSet rs = null;
        Connection conn = getConnection();
        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();
        } catch (SQLException e) {
            logger.error("exeQuery出现错误",e);
            throw e;
        }
        return rs;

    }
}
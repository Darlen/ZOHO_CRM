/** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 *    ProjectName ZOHO_CRM
 *    File Name   Rows.java 
 * ** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 *    Copyright (c) 2016 Darlen . All Rights Reserved. 
 *    注意： 本内容仅限于XXX公司内部使用，禁止转发
 * ** ** ** ** ** ** ** **** ** ** ** ** ** ** **** ** ** ** ** ** ** **
 * */
package darlen.crm.jaxb_xml_object.SO;

import javax.xml.bind.annotation.*;
import java.util.List;

/**
 * darlen.crm.jaxb_xml_object.leads
 * Description：ZOHO_CRM
 * Created on  2016/08/27 08：26
 * -------------------------------------------------------------------------
 * 版本     修改时间        作者         修改内容 
 * 1.0.0        08：26   Darlen              create
 * -------------------------------------------------------------------------
 *
 * @author Darlen liu
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class Row {
    @XmlAttribute(name="no")
    private Integer no;

    @XmlElement(name = "FL")
    private List<FL> fls;
    /**在这里暂时先把Product Details的FL转换成pds*/
    @XmlElement(name = "pds")
    private ProdDetails pds;
//private String fl;
    public Integer getNo() {
        return no;
    }

    public void setNo(Integer no) {
        this.no = no;
    }

    /*public String getFl() {
        return fl;
    }

    public void setFl(String fl) {
        this.fl = fl;
    }*/
   /* public FL getFl() {
        return fl;
    }

    public void setFl(FL fl) {
        this.fl = fl;
    }*/

    public List<FL> getFls() {
        return fls;
    }

    public void setFls(List<FL> fl) {
        this.fls = fl;
    }

    public ProdDetails getPds() {
        return pds;
    }

    public void setPds(ProdDetails pds) {
        this.pds = pds;
    }

    @Override
    public String toString() {
        return "Row{" +
                "no=" + no +
                ", fls=" + fls +
                ", pds=" + pds +
                '}';
    }
}

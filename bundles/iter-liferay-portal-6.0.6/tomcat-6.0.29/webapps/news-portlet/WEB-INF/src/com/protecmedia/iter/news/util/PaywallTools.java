package com.protecmedia.iter.news.util;

import java.util.ArrayList;
import java.util.List;

import com.liferay.portal.kernel.error.ServiceError;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.CategoriesUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.DocumentException;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portal.kernel.xml.XMLHelper;
import com.protecmedia.iter.news.paywall.model.PaywallProductModel;
import com.protecmedia.iter.news.service.ProductLocalServiceUtil;

public class PaywallTools
{
	long groupId;
	List<PaywallProductModel> products;
	PaywallProductModel selectedProduct;
	
	public PaywallTools(long groupId, String selectedProductName, String errorPageFriendlyUrl) throws NoSuchMethodException, SecurityException, DocumentException, ServiceError, PortalException, SystemException
	{
		this.groupId = groupId;
		this.products = new ArrayList<PaywallProductModel>();
		this.selectedProduct = null;
		
		// Carga los productos
		Document docProducts = SAXReaderUtil.read(ProductLocalServiceUtil.getPaywallProducts(String.valueOf(groupId)));
		List<Node> productsNodes = docProducts.selectNodes("/rs/row");
		if (productsNodes.size() > 0)
		{
			for (Node productNode : productsNodes)
			{
				if (XMLHelper.getLongValueOf(productNode, "@pnoreg") == 0)
				{
					// Crea el producto
					PaywallProductModel paywallProduct = new PaywallProductModel(productNode, errorPageFriendlyUrl);
					// Lo añade a la lista
					products.add(paywallProduct);
					// Comprueba si es el producto seleccionado
					if (Validator.isNotNull(selectedProductName) && selectedProductName.equals(CategoriesUtil.normalizeText(paywallProduct.get("name").toString())))
						selectedProduct = paywallProduct;
				}
			}
		}
	}
	
	public List<PaywallProductModel> getProducts()
	{
		return products;
	}
	
	public PaywallProductModel getSelectedProduct()
	{
		return selectedProduct;
	}
}

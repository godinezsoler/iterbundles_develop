package com.protecmedia.iter.news.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.xml.Document;
import com.liferay.portal.kernel.xml.Element;
import com.liferay.portal.kernel.xml.Node;
import com.liferay.portal.kernel.xml.ProcessingInstruction;
import com.liferay.portal.kernel.xml.SAXReaderUtil;
import com.liferay.portlet.asset.NoSuchVocabularyException;
import com.liferay.portlet.asset.model.AssetCategory;
import com.liferay.portlet.asset.model.AssetVocabulary;
import com.liferay.portlet.asset.service.AssetCategoryLocalServiceUtil;
import com.liferay.portlet.asset.service.AssetVocabularyLocalServiceUtil;

public class GetVocabulary extends HttpServlet implements Servlet
{
	private static final long	serialVersionUID	= 1L;
	private static Log _log = LogFactoryUtil.getLog(GetVocabulary.class);
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
	{
		String result = StringPool.PERIOD;
		
		try
		{
			String[] path = request.getPathInfo().split(StringPool.SLASH);
			
			long vocabularyId = 0;
			
			try
			{
				vocabularyId = Long.parseLong(path[1]);
			}
			catch (Throwable th)
			{
				_log.error(th);
				addResponse(response, HttpServletResponse.SC_BAD_REQUEST, result);
			}
			
			if (vocabularyId > 0)
			{
				AssetVocabulary vocabulary = AssetVocabularyLocalServiceUtil.getVocabulary(vocabularyId);
				
				Document categoryHierarchy = SAXReaderUtil.read("<vocabulary />");
				Element root = categoryHierarchy.getRootElement();
				root.addAttribute("name", vocabulary.getName());
				root.addAttribute("id", String.valueOf(vocabulary.getVocabularyId()));
				
				List<AssetCategory> categories = AssetCategoryLocalServiceUtil.getVocabularyCategories(vocabularyId, QueryUtil.ALL_POS, QueryUtil.ALL_POS, null);
				
				// Construye un XML plano con las categorías
				for (AssetCategory category : categories)
				{
					Element element = root.addElement("category");
					element.addAttribute("name", category.getName());
					element.addAttribute("id", String.valueOf(category.getCategoryId()));
					if (category.getParentCategoryId() > 0)
						element.addAttribute("parent", String.valueOf(category.getParentCategoryId()));
				}
				
				// Construye el arbol
				buildTree(root);
				// Añade el PHP para cambiar el content-type
				ProcessingInstruction phpHeaderXML = SAXReaderUtil.createProcessingInstruction("php", "header(\"Content-Type: text/xml\");");
				categoryHierarchy.add(phpHeaderXML);
				// Crea la respuesta
				addResponse(response, HttpServletResponse.SC_OK, categoryHierarchy.asXML());
			}
		}
		catch (NoSuchVocabularyException e)
		{
			_log.error(e);
			addResponse(response, HttpServletResponse.SC_NOT_FOUND, result);
		}
		catch (Throwable th)
		{
			_log.error(th);
			addResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, result);
		}
	}
	
	private void buildTree(Element root)
	{
		List<Node> categories = root.selectNodes("/vocabulary/category[@parent!=0]");
		// Para cada categoría que no sea raiz
		for (Node category : categories)
		{
			// Busca a su padre
			Node parent = root.selectSingleNode("//category[@id=" + ((Element) category).attributeValue("parent") + "]");
			if (parent != null)
			{
				category.detach();
				((Element) category).remove(((Element) category).attribute("parent"));
				((Element) parent).add(category);
			}
		}
	}	
	
	private void addResponse(HttpServletResponse response, int status, String output )
	{
		response.setStatus(status);
		
		PrintWriter out = null;
		if(Validator.isNotNull(output))
		{
			response.setContentType("application/x-getvocabulary");
			response.setCharacterEncoding(StringPool.UTF8);
			response.setHeader("ITER-ResponseNeedsPHP", "1");
			try
			{
				out = response.getWriter();
			    out.println(output);
				out.flush();
			}
			catch (IOException e)
			{
				_log.error(e);
			}
			finally
			{
				if (out != null)
					out.close();
			}
		}
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		
	}
}

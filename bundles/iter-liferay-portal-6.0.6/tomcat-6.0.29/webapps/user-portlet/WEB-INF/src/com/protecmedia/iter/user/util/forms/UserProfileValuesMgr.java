package com.protecmedia.iter.user.util.forms;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang.StringUtils;

import com.liferay.portal.kernel.util.IterUserTools;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.PortalLocalServiceUtil;
import com.protecmedia.iter.base.service.util.ServiceError;
import com.protecmedia.iter.user.util.IterRegisterQueries;

public class UserProfileValuesMgr
{
	private List<String> 	_list2Insert = null;
	private List<String> 	_list2Delete = null;
	
	public UserProfileValuesMgr()
	{
		_list2Insert = new ArrayList<String>();
		_list2Delete = new ArrayList<String>();
	}
	
	public void add2Insert(String profileFieldId, String fieldValue, String binFieldValueId) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException
	{
		if (Validator.isNull(fieldValue))
			fieldValue = StringPool.NULL;
		else
			fieldValue = IterUserTools.encryptGDPR_Quoted(fieldValue);
		_list2Insert.add( String.format(IterRegisterQueries.VALUES_FOR_USER_PROFILE, "%1$s", profileFieldId, fieldValue, binFieldValueId) );
	}
	
	public void add2Delete(String profileFieldId)
	{
		_list2Delete.add(profileFieldId);
	}
	
	public void doInsert(String userId) throws IOException, SQLException, ServiceError
	{
		if ( _list2Insert.size() > 0 )
		{
			String values = String.format( StringUtils.join(_list2Insert.toArray(new String[_list2Insert.size()]), StringPool.COMMA), userId);
			PortalLocalServiceUtil.executeUpdateQuery( String.format(IterRegisterQueries.INSERT_USER_PROFILE_VALUES, values) );
		}
	}
	
	public void doDelete(String userId) throws IOException, SQLException
	{
		if (Validator.isNotNull(userId) && _list2Delete.size() > 0)
		{
			String query = String.format(IterRegisterQueries.DELETE_USER_PROFILE_VALUES, userId);
			
			query = query.concat( String.format(IterRegisterQueries.DELETE_USER_PROFILE_VALUES_PROFILECLAUSE, StringUtil.merge(_list2Delete.toArray(new String[_list2Delete.size()]), "','")) );
			PortalLocalServiceUtil.executeUpdateQuery(query);
		}
	}

}

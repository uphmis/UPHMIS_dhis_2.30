package org.hisp.dhis.reports.action;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.hisp.dhis.organisationunit.OrganisationUnit;
import org.hisp.dhis.organisationunit.OrganisationUnitService;
import org.hisp.dhis.reports.ReportService;
import org.hisp.dhis.security.PasswordManager;
import org.hisp.dhis.user.User;
import org.hisp.dhis.user.UserAuthorityGroup;
import org.hisp.dhis.user.UserCredentials;
import org.hisp.dhis.user.UserService;

import com.opensymphony.xwork2.Action;

public class AddMultipleUserAction
    implements Action
{
    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private ReportService reportService;

    public void setReportService( ReportService reportService )
    {
        this.reportService = reportService;
    }
    /*
    private UserStore userStore;

    public void setUserStore( UserStore userStore )
    {
        this.userStore = userStore;
    }
    */
    private PasswordManager passwordManager;

    public void setPasswordManager( PasswordManager passwordManager )
    {
        this.passwordManager = passwordManager;
    }
    
    private OrganisationUnitService organisationUnitService;

    public void setOrganisationUnitService( OrganisationUnitService organisationUnitService )
    {
        this.organisationUnitService = organisationUnitService;
    }
    
    private UserService userService;
    
    public void setUserService( UserService userService )
    {
        this.userService = userService;
    }
    
    // -------------------------------------------------------------------------
    // Input/Output
    // -------------------------------------------------------------------------
    private String message;

    public String getMessage()
    {
        return message;
    }

    public void setMessage( String message )
    {
        this.message = message;
    }

    
    private InputStream inputStream;

    public InputStream getInputStream()
    {
        return inputStream;
    }

    private String contentType;

    public String getContentType()
    {
        return contentType;
    }

    public void setUploadContentType( String contentType )
    {
        this.contentType = contentType;
    }

    private int bufferSize;

    public int getBufferSize()
    {
        return bufferSize;
    }

    private File file;

    public void setUpload( File file )
    {
        this.file = file;
    }

    private String fileName;

    public String getFileName()
    {
        return fileName;
    }

    public void setUploadFileName( String fileName )
    {
        this.fileName = fileName;
    }

    private List<String> userImportStatusMsgList = new ArrayList<String>();
    
    public List<String> getUserImportStatusMsgList()
    {
        return userImportStatusMsgList;
    }
    
    private String excelFilePath = "";
    
    // -------------------------------------------------------------------------
    // Action implementation
    // -------------------------------------------------------------------------
    public String execute()
        throws Exception
    {
        System.out.println( "User  Creation Start Time is : " + new Date() );
        
        String raFolderName = reportService.getRAFolderName();
        
        String excelImportFolderName = "excelimport";
        
        excelFilePath = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator
            + excelImportFolderName + File.separator + "pending" + File.separator + fileName;

        file.renameTo( new File( excelFilePath ) );
        moveFile( file, new File( excelFilePath ) );
        
        String fileType = fileName.substring(fileName.indexOf( '.' )+1, fileName.length());
        
        if (!fileType.equalsIgnoreCase( "xls" ))
        {
            message = "The file you are trying to import is not an excel file";
            
            return SUCCESS;
        }
        
        else
        {
           populateUserFromExcel();
        }
        
        //String fileName = "user.xls";
        
        //String excelFilePath = System.getenv( "DHIS2_HOME" ) + File.separator + raFolderName + File.separator + excelImportFolderName + File.separator  + fileName;
        System.out.println( "User  Creation End Time is : " + new Date() );
 
        return SUCCESS;
    }
    
    public void populateUserFromExcel() throws IOException, BiffException
    {
        //Workbook templateWorkbook = Workbook.getWorkbook( new File( excelFilePath ) );
        //WritableWorkbook outputReportWorkbook = Workbook.createWorkbook( new File( excelFilePath ), templateWorkbook );
        
        int sheetNo = 0 ;
        Workbook excelImportFile = Workbook.getWorkbook( new File( excelFilePath ) );
        Sheet sheet0 = excelImportFile.getSheet( sheetNo );
                
        //WritableSheet sheet0 = outputReportWorkbook.getSheet( sheetNo );
        Integer rowStart = Integer.parseInt( sheet0.getCell( 8, 0 ).getContents() );
        Integer rowEnd = Integer.parseInt( sheet0.getCell( 8, 1 ).getContents() );
        //System.out.println( "User  Creation Start Time is : " + new Date() );
        //System.out.println( "Row Start : " + rowStart + " ,Row End : "  + rowEnd );
        int orgunitcount = 0;
        try
        {
            for( int i = rowStart ; i <= rowEnd ; i++ )
            {
                int rowNo = i + 1;
                Integer orgUnitId = Integer.parseInt( sheet0.getCell( 0, i ).getContents() );
                String orgUnitname = sheet0.getCell( 1, i ).getContents();
                //String orgUnitCode = sheet0.getCell( 2, i ).getContents();
                String userName = sheet0.getCell( 3, i ).getContents();
                String passWord = sheet0.getCell( 4, i ).getContents();
                Integer userRoleId = Integer.parseInt( sheet0.getCell( 5, i ).getContents() );
                
                //System.out.println(  rowStart + " --" + orgUnitId +"--" + orgUnitname + "--" + userName + " --" + passWord +"--" + userRoleId );
                
                OrganisationUnit orgUId = organisationUnitService.getOrganisationUnit( orgUnitId );
                Set<OrganisationUnit> orgUnits = new HashSet<OrganisationUnit>();
                orgUnits.add( orgUId );
                
                Collection<User> tempUserList = orgUId.getUsers();
                int flag = 0;
                if ( tempUserList != null )
                {
                    for ( User u : tempUserList )
                    {
                        //UserCredentials uc = userStore.getUserCredentials( u );
                        //UserCredentials uc = userService.getUserCredentials( u );
                        UserCredentials uc = userService.getUserCredentialsByUsername( u.getUsername() );
                        if ( uc != null && uc.getUsername().equalsIgnoreCase( userName ) )
                            //System.out.println( userName + " ALREADY EXITS" );
                            flag = 1;
                    }
                }
                if ( flag == 1 )
                {
                    userImportStatusMsgList.add( " Row No - " + rowNo + " User  " + userName + "  ALREADY EXITS ");
                    
                    //System.out.println( userName + " ALREADY EXITS inside flag 1 " );
                    continue;
                }
                
                User user = new User();
                user.setSurname( orgUnitname );
                user.setFirstName( userName );
                user.setOrganisationUnits( orgUnits );
                
                UserCredentials userCredentials = new UserCredentials();
                userCredentials.setUser( user );
                userCredentials.setUsername( userName );
                
                //userCredentials.setPassword( passwordManager.encodePassword( userName, passWord ) );
                userCredentials.setPassword( passwordManager.encode( passWord ) );
                
                UserAuthorityGroup group = userService.getUserAuthorityGroup( userRoleId );
                
                //UserAuthorityGroup group = userStore.getUserAuthorityGroup( userRoleId );
                userCredentials.getUserAuthorityGroups().add( group );
    
                //userStore.addUser( user );
                //userStore.addUserCredentials( userCredentials );
                
                userService.addUser( user );
                userService.addUserCredentials( userCredentials );
                userImportStatusMsgList.add( " Row No - " + rowNo +". is succuessfully imported." + " User  " + userName );
                //System.out.println( orgUnitname + " Created" );
                orgunitcount++;
            }
        }
        
        catch ( Exception e )
        {
            message = "Exception occured while importing users, please check log for more details" + e.getMessage();
        }
        
        //System.out.println( "**********************************************" );
        //System.out.println( "MULTIPLE USER CREATION IS FINISHED" );
        //System.out.println( "Total No of User Created : -- " + orgunitcount );
        //System.out.println( "**********************************************" );
        
        userImportStatusMsgList.add( "Total No of User Created : -- " + orgunitcount );
        
        excelImportFile.close();

    }
    
    // Supportive Methods
    public int moveFile( File source, File dest ) throws IOException
    {
        if ( !dest.exists() )
        {
            dest.createNewFile();
        }

        InputStream in = null;

        OutputStream out = null;

        try
        {

            in = new FileInputStream( source );

            out = new FileOutputStream( dest );

            byte[] buf = new byte[1024];

            int len;

            while ( (len = in.read( buf )) > 0 )
            {
                out.write( buf, 0, len );
            }
        }

        catch ( Exception e )
        {
            return -1;
        }

        finally
        {
            if ( in != null )
            {
                in.close();
            }
                
            if ( out != null )
            {
                out.close();
            }
                
        }
        return 1;
    }
    
}

#!/usr/bin/perl

#############################################################################
# File: execAssm.pl
#
# Soderlund Lab, University of Arizona, 2010
#
# Do a TCW assembly. 
# Uses a Java module, for which maximum memory must first be set. 
#
#############################################################################

# Maximum memory to use, in megabytes.
# If zero, it will be automatically calculated based on the size of the project.
# In order to use more than 2Gb, you must be using 64-bit Java. 
my $maxmem = 50000;

use warnings;
use strict;
$| = 1;
use DBI;
use Cwd;
use Cwd 'abs_path';
use File::Path;

# Check that runSingleTCW is run from top level only

my $PROJECTS_DIR = 'projects';

if (not -d $PROJECTS_DIR )
{
	die "Please run execAssm.pl from the top level TCW directory only\n";
}

my %Params;  #values from configuration files
our $TCWcfg = 'sTCW.cfg'; 
my $project_name = ''; 

#############################################################
# Get command-line parameters
if (0 == scalar @ARGV || $ARGV[0] =~ /^help$/i) {
	print "\nUsage: execAssm.pl [-q] <directory> [sTCW.cfg]\n";
	print "	<directory> must be under /projects.\n";
	print "Run the assembly specified in <directory>/sTCW.cfg.\n";
	print "Optional -q runs without prompting.\n";
	exit;
}

my $argStart = 0;
if ($ARGV[0] eq "-q") 
{
	$argStart = 1;
}
$project_name = $ARGV[$argStart];

if (1 + $argStart < scalar @ARGV) {
	$TCWcfg = $ARGV[1 + $argStart];
}

$TCWcfg = "$PROJECTS_DIR/$project_name/$TCWcfg";
die "Config file $TCWcfg does not exist.\n" unless -f $TCWcfg;
my $HOSTScfg = "HOSTS.cfg";

readCfgFile($TCWcfg,\%Params);
if (not defined $Params{SingleID})
{
	die "Config file missing SingleID\n";
}
if (not defined $Params{STCW_db})
{
	die "Config file missing STCW_db\n";
}
my $AssemblyID = $Params{SingleID}; 
my $TCW_db = $Params{STCW_db};

completeDBParams(\%Params,$HOSTScfg);


# TCW database parameters
my $TCW_usr = $Params{TCW_user} || "";
my $TCW_host = $Params{TCW_host} || "";
my $TCW_password = $Params{TCW_password} || "";
#Make sure TCW DB is there 

my $dbh = DBI->connect( "dbi:mysql:$TCW_db;host=$TCW_host", "$TCW_usr", "$TCW_password",{ PrintError => 0} ) 
		or die "*** could not locate database $TCW_db on $TCW_host (are the DB parameters in HOSTS.cfg correct?).\n";  

my $exists = 0;
my $sqlCheck = qq{select assemblyid from assembly where assemblyid = ?};
my $sthCheck = $dbh->prepare($sqlCheck);
$sthCheck->execute( $AssemblyID );
$sthCheck->bind_columns( \$exists );
$sthCheck->fetch;
$sthCheck->finish;

if ($maxmem == 0)
{

    my $sth = $dbh->prepare("select count(*) from clone");
    $sth->execute();
    ((my $numClones) = $sth->fetchrow_array());

    $sth = $dbh->prepare("select sum(libsize*avglen)/sum(libsize) from library");
    $sth->execute();
    ((my $avgLen) = $sth->fetchrow_array());

	$maxmem = int($numClones/200);
	if ($avgLen > 300)
	{
		my $lenfact = $avgLen/300;
		$maxmem = int($maxmem*$lenfact);
	}
   $maxmem += 1000; 
   if ($numClones > 3000000)
	{
		print "*Warning: $numClones EST found in database. Estimated memory requirement: $maxmem"."M".".\n" ;
		print "*See System Guide for more information on memory usage.\n" ;
		sleep(2);
	} 
    print "Memory set to $maxmem\n";
	$ENV{"TCW_MEM"} = $maxmem;
}

my $jardir = "java/jars";
my $jar = "$jardir/stcw.jar";

if (not -f $jar)
{
	die "can't find $jar; make sure to run from the TCW directory.\n";
}

my $classpath = "$jar:$jardir/mysql-connector-java-5.0.5-bin.jar";
my $cmd = "java -Xmx${maxmem}m -cp $classpath sng.assem.AssemMain ".join(" ",@ARGV);

print "Command ".$cmd;
system($cmd);

#######################################################
## duplicated in execLoad.pl

sub readCfgFile
{
	my $file = shift;
   	my $pParm = shift;
    
   	open IF, "$file" or &shared::exit_w_error($_, "Failed to locate $file");
    while (<IF>) {
        my ($line) = split /\#/;
        next if (!$line);
        chomp $line;
        next if not $line =~ /=/;
        my ($param, $val) = split /\s*=\s*/, $line;
        $param =~ s/^\s+//;
        $val =~ s/^\s+//;
        $param =~ s/\s+$//;
        $val =~ s/\s+$//;
        $pParm->{$param} = ( $val ? $val : "");

    }
}

#############################################################

sub parseHosts
{
	my $file = shift;
    my $pSecs = shift;
    
    my %curSec;
    my %expected = (TCW_host => 1, TCW_user => 1, TCW_password => 1,
    		DB_host => 1, DB_user => 1, DB_password => 1);
    
    open F, $file or die;
    
    while (<F>)
    {
        my ($line) = split /\#/;
        next if (!$line);
        chomp $line;
        next if not $line =~ /=/;
        my ($param, $val) = split /\s*=\s*/, $line;
        $param =~ s/\s+//g;
        $val =~ s/\s+//g;
        if (defined $expected{$param})
        {
        	$param =~ s/^DB_/TCW_/;
			$curSec{$param} = $val;
            if (3 == scalar keys %curSec)
            {
            	my $host = $curSec{TCW_host};
                foreach my $key (keys %expected)
                {
                	$pSecs->{$host}{$key} = $curSec{$key};
                }
                if (1 == scalar keys %$pSecs) # First section is the default
                {
                    foreach my $key (keys %expected)
                    {
                	    $pSecs->{"__DeFaUlT"}{$key} = $curSec{$key};
                    }
                
                }

                
                %curSec = ();
            }
		}            
    }
        
}

#############################################################

sub completeDBParams
{
	my $pParams = shift;
    my $HOSTScfg = shift;
    
    if ( (not defined $pParams->{TCW_user}) || $pParams->{TCW_user} eq ""  ||
		    (not defined $pParams->{TCW_host}) || $pParams->{TCW_host} eq ""  )
    {
	    #print "User/host not specified in TCW.cfg.\n";
        if (-f $HOSTScfg)
        {
    	    #print "Looking in HOSTS.cfg\n";
            my %secs;
            parseHosts($HOSTScfg,\%secs);
            my $nEntry = scalar keys %secs;
            if (0) #$nEntry == 1)
            {
            	my $host = (keys %secs)[0];
                my $user = $secs{$host}{TCW_user};
                my $pass = $secs{$host}{TCW_password};
        	    print "Using host=$host,user=$user,password=$pass\n";
        	    $pParams->{TCW_host} = $host;
        	    $pParams->{TCW_user} = $user;
        	    $pParams->{TCW_password} = $pass;
            }
            elsif (defined $pParams->{TCW_host} && defined $secs{$pParams->{TCW_host}})
           	{
            	my $host = $pParams->{TCW_host};
                my $user = $secs{$host}{TCW_user};
                my $pass = $secs{$host}{TCW_password};
        	    print "Using host=localhost,user=$user,password=$pass\n";
        	    $pParams->{TCW_user} = $user;
        	    $pParams->{TCW_password} = $pass;            	
            }
            else
            {
            	#print "HOSTS.cfg has $nEntry entries.\n";
                my $host = hostName();
                if ($host eq "")
                {
                	print "Unable to get local hostname\n";
                    print "Enter full database information into sTCW.cfg and LIB.cfg\n";
                }
                else
                {
                	#print "Using local host $host\n";
                	if (defined $secs{$host})
                    {
                        my $user = $secs{$host}{TCW_user};
                        my $pass = $secs{$host}{TCW_password};
        	            #print "Using host=localhost,user=$user,password=$pass\n";
        	            $pParams->{TCW_host} = "localhost";
        	            $pParams->{TCW_user} = $user;
        	            $pParams->{TCW_password} = $pass;
                    }
                	elsif (defined $secs{"__DeFaUlT"})
                    {
                        my $user = $secs{"__DeFaUlT"}{TCW_user};
                        my $pass = $secs{"__DeFaUlT"}{TCW_password};
        	            #print "Using host=localhost,user=$user,password=$pass\n";
        	            $pParams->{TCW_host} = "localhost";
        	            $pParams->{TCW_user} = $user;
        	            $pParams->{TCW_password} = $pass;
                    }
                    
                    else
                    {
                    	print "Local host ($host) not found in HOSTS.cfg.\n ";
                        exit(0);
                    }
                }
            }
        }
        else
        {
        	print "Can't find HOSTS.cfg\n";
    	    exit(0);
        }
    }
}    

#############################################################

sub hostName
{
	my $full = $ENV{HOSTNAME} || `uname -n`;
    return "" if not defined $full;
    #$full =~ s/\..*//;
    return $full; 
}


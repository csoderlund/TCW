#!/usr/bin/perl

#############################################################################
# File: execLoad.pl
#
# Soderlund Lab, University of Arizona, 2010
#
# Load a TCW library. 
#
#############################################################################

# Maximum memory to use, in megabytes.
# A higher value may be necessary for very large projects.
my $maxmem = 2000;

use warnings;
use strict;
$| = 1;
use DBI;
use Cwd;
use Cwd 'abs_path';
use File::Path;

# Check that is run from top level only

my $LIB_DIR = 'projects';

if (not -d $LIB_DIR )
{
	die "Please run execLoadLibrary.pl from the top level TCW directory only\n";
}

my %Params;  #values from configuration files
our $TCWcfg = 'LIB.cfg'; 
my $project_name = ''; 

#############################################################
# Get command-line parameters
if (0 == scalar @ARGV || $ARGV[0] =~ /^help$/i) {
	print "\nUsage: execLoadLibrary.pl [-q] <directory> \n";
	print "	<directory> must be under /libraries.\n";
	print "Load the libraries specified in <directory>/LIB.cfg.\n";
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


sub listdirs {
	opendir(PRODIR, "$LIB_DIR/");
	my @prosubdirs = grep { /.*/} readdir(PRODIR);
	closedir(PRODIR);
	if (2 < scalar @prosubdirs) {
		print "The following project directories are under the /$LIB_DIR:\n";
		foreach my $sd (sort @prosubdirs) {
			if (-d "$LIB_DIR/$sd") {
				print "   $sd\n" unless($sd eq '.' || $sd eq '..');
			} 
		}
	}
}

$TCWcfg = "$LIB_DIR/$project_name/$TCWcfg";
die "Config file $TCWcfg does not exist.\n" unless -f $TCWcfg;
my $HOSTScfg = "HOSTS.cfg";

readCfgFile($TCWcfg,\%Params);
if (not defined $Params{STCW_db})
{
	die "sTCW.cfg missing STCW_db\n";
}
my $TCW_db = $Params{TCW_db};

completeDBParams(\%Params,$HOSTScfg);

# TCW database parameters
my $TCW_usr = $Params{TCW_user};
my $TCW_host = $Params{TCW_host};
my $TCW_password = $Params{TCW_password};
#Make sure TCW DB is there 
my $dbh = DBI->connect( "dbi:mysql:mysql;host=$TCW_host", "$TCW_usr", "$TCW_password",
		{ PrintError => 0, ChopBlanks => 1, LongReadLen => 100000 } ) 
		or die "*** could not locate MySQL database on $TCW_host (are the username/password in LIB.cfg correct?).\n";  

my $jardir = "java/jars";
my $jar = "$jardir/stcw.jar";

if (not -f $jar)
{
	die "can't find $jar; make sure to run from the TCW directory.\n";
}

my $classpath = "$jar:$jardir/mysql-connector-java-5.0.5-bin.jar";
my $cmd = "java -Xmx${maxmem}m -cp $classpath sng.assem.LoadLibMain ".join(" ",@ARGV);

#print "execute:$cmd\n";
system($cmd);

#######################################################
## duplicated in execAssm.pl

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

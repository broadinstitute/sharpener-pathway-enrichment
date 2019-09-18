#!/usr/bin/perl -w

use File::Basename;

$dirname   = dirname(__FILE__);
$python3   = "python3";

die "Usage: $0 MAX_GENES PWAY_PV GENE_PV gene_list\n" if @ARGV < 4;
$COUNT   = $ARGV[0];
$PWAY_PV = $ARGV[1];
$GENE_PV = $ARGV[2];
$genes   = $ARGV[3];

$genes =~ s/NCBIGene://g;
$genes =~ s/ncbigene://g;
foreach(split /,/, $genes) { $myGenes{$_} = 1 }

open(IN,"$dirname/uniprot2ncbi.txt") || die "Unable to open uniprot2ncbi.txt\n";
while(<IN>) {
	s/[\r\n]+//g;
	($uniprot,$ncbi) = split /\t/, $_;
	$uniprot2ncbi{$uniprot} = $ncbi;
}

#run pathDIP and output
open(FROM,"echo $genes | $python3 $dirname/pathDIP.py |") || die "Error while running pathDIP\n";

#read pathways
while(<FROM>) { last if /^Pathway/ }
while(<FROM>) {
	s/[\r\n]+//g;
	last if $_ eq "";
	@data = split /\t/, $_;
	$pathway = "$data[0]-$data[1]";
	$pathway2pval{$pathway} = $data[4];
}

#read genes
while(<FROM>) { last if /^UniProt/ }
while(<FROM>) {
	s/[\r\n]+//g;
	@data = split /\t/, $_;
	last if $_ eq "";
	$pathway = "$data[3]-$data[4]";
	$pathway2pval{$pathway} = 1 if !exists $pathway2pval{$pathway};
	$pval = $data[11];
	foreach $uniprot (split /,/, $data[9]) {
		next if !exists $uniprot2ncbi{$uniprot};
		$gene = $uniprot2ncbi{$uniprot};
		if(!exists $gene2pval{$gene} || $pval < $gene2pval{$gene}) {
			$gene2pathway{$gene} = $pathway;
			$gene2pval{$gene} = $pval;
		}
	}
}

foreach $g (keys %myGenes) {
	if(!exists $gene2pval{$g}) {
		$gene2pval{$g} = 1.0;
		$gene2pathway{$g} = "";
		$pathway2pval{$gene2pathway{$g}} = 1.0;
	}
}

#foreach $g (sort { $pathway2pval{$gene2pathway{$a}} <=> $pathway2pval{$gene2pathway{$b}} || $gene2pval{$a} <=> $gene2pval{$b} || $a <=> $b } keys %myGenes) {
#	print "$g\t$gene2pathway{$g}\t$pathway2pval{$gene2pathway{$g}}\t$gene2pval{$g}\n";
#}

$count = 0;
foreach $g (sort { $pathway2pval{$gene2pathway{$a}} <=> $pathway2pval{$gene2pathway{$b}} || $gene2pval{$a} <=> $gene2pval{$b} || $a <=> $b } keys %gene2pval) {
	#next if exists $myGenes{$g};
	next if $pathway2pval{$gene2pathway{$g}} > $PWAY_PV;
	next if    $gene2pval{$g}                > $GENE_PV;
	print "$g\t$gene2pathway{$g}\t$pathway2pval{$gene2pathway{$g}}\t$gene2pval{$g}\n";
	last if ++$count >= $COUNT;
}


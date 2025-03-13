## Load tests
#
# Load tests are run using siege
#
# Link: https://www.joedog.org/siege-home/
# Installation with homebrew: https://formulae.brew.sh/formula/siege
# Or download from: http://download.joedog.org/siege/siege-latest.tar.gz
#
# To change what urls are used, edit ./siege/etc/urls.txt

# Runs siege with 25 workers, a randomized delay of around 0.3s and 100 repetitions per worker
siege -c25 -d0.3 -r100 --file=./siege/etc/urls.txt
